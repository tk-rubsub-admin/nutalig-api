package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.constant.Currency;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderPaymentRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderPaymentRequest;
import com.nutalig.dto.PurchaseOrderPaymentDto;
import com.nutalig.entity.PurchaseOrderAttachmentEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.PurchaseOrderPaymentMapper;
import com.nutalig.repository.PurchaseOrderPaymentRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseOrderPaymentService {

    private static final Set<PurchaseOrderPaymentStatus> COMMITTED_STATUSES = EnumSet.of(
            PurchaseOrderPaymentStatus.PENDING,
            PurchaseOrderPaymentStatus.APPROVED
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderPaymentRepository purchaseOrderPaymentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PurchaseOrderPaymentMapper purchaseOrderPaymentMapper;
    private final ActivityHistoryService activityHistoryService;
    private final PurchaseOrderPaymentScheduleService purchaseOrderPaymentScheduleService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderPaymentDto> getPayments(String purchaseOrderNo) throws DataNotFoundException {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        return purchaseOrder.getPayments().stream()
                .sorted(Comparator.comparing(PurchaseOrderPaymentEntity::getCreatedDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PurchaseOrderPaymentEntity::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(purchaseOrderPaymentMapper::toDto)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentDto createPayment(
            String purchaseOrderNo,
            CreatePurchaseOrderPaymentRequest request,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        String requestKey = StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getRequestKey()), UUID.randomUUID().toString());
        Optional<PurchaseOrderPaymentEntity> existingPayment = purchaseOrderPaymentRepository.findByRequestKey(requestKey);
        if (existingPayment.isPresent()) {
            if (!StringUtils.equals(existingPayment.get().getPurchaseOrder().getPurchaseOrderNo(), purchaseOrderNo)) {
                throw new InvalidRequestException("requestKey has already been used for another purchase order");
            }
            return purchaseOrderPaymentMapper.toDto(existingPayment.get());
        }

        PurchaseOrderEntity purchaseOrder = getPurchaseOrderForUpdate(purchaseOrderNo);
        ensurePaymentAllowed(purchaseOrder);
        PurchaseOrderPaymentScheduleEntity schedule = resolveSchedule(purchaseOrder, request.getScheduleId());
        PurchaseOrderPaymentType paymentType = schedule == null ? request.getPaymentType() : schedule.getPaymentType();
        Integer installmentNo = schedule == null ? request.getInstallmentNo() : schedule.getInstallmentNo();
        validatePaymentRequest(
                paymentType,
                installmentNo,
                request.getPaymentDate(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getChequeBank(),
                request.getChequeNo(),
                request.getChequeDate(),
                attachments,
                true
        );
        validateCommittedLimit(purchaseOrder, schedule, request.getAmount(), null);
        BigDecimal exchangeRate = resolvePaymentExchangeRate(purchaseOrder, request.getExchangeRate());

        UserEntity user = getUser(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        PurchaseOrderPaymentEntity payment = new PurchaseOrderPaymentEntity();
        applyPaymentValues(
                payment,
                paymentType,
                installmentNo,
                request.getPaymentDate(),
                request.getAmount(),
                exchangeRate,
                request.getPaymentMethod(),
                request.getTransferReference(),
                request.getChequeBank(),
                request.getChequeNo(),
                request.getChequeDate(),
                request.getChequeBranch(),
                request.getRemark(),
                purchaseOrder.getCurrency()
        );
        payment.setStatus(PurchaseOrderPaymentStatus.PENDING);
        payment.setRequestKey(requestKey);
        payment.setCreatedBy(user);
        payment.setUpdatedBy(user);
        payment.setCreatedDate(now);
        payment.setUpdatedDate(now);
        purchaseOrder.addPayment(payment);
        assignSchedule(payment, schedule);
        addAttachments(payment, attachments, user, now, purchaseOrderNo);
        purchaseOrder.setUpdatedBy(user);
        purchaseOrder.setUpdatedDate(now);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        recordActivity(purchaseOrder, payment, userId, ActivityAction.CREATE,
                "บันทึกรายการชำระเงินของใบสั่งซื้อเลขที่ " + purchaseOrderNo, null);
        return purchaseOrderPaymentMapper.toDto(payment);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentDto updatePayment(
            String purchaseOrderNo,
            Long paymentId,
            UpdatePurchaseOrderPaymentRequest request,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        PurchaseOrderEntity purchaseOrder = getPurchaseOrderForUpdate(purchaseOrderNo);
        ensurePaymentAllowed(purchaseOrder);
        PurchaseOrderPaymentEntity payment = getPayment(purchaseOrderNo, paymentId);
        if (payment.getStatus() != PurchaseOrderPaymentStatus.PENDING) {
            throw new InvalidRequestException("Only pending purchase order payments can be edited");
        }
        PurchaseOrderPaymentScheduleEntity schedule = resolveSchedule(purchaseOrder, request.getScheduleId());
        PurchaseOrderPaymentType paymentType = schedule == null ? request.getPaymentType() : schedule.getPaymentType();
        Integer installmentNo = schedule == null ? request.getInstallmentNo() : schedule.getInstallmentNo();
        validatePaymentRequest(
                paymentType, installmentNo, request.getPaymentDate(),
                request.getAmount(), request.getPaymentMethod(), request.getChequeBank(),
                request.getChequeNo(), request.getChequeDate(), attachments, false
        );
        validateCommittedLimit(purchaseOrder, schedule, request.getAmount(), paymentId);
        BigDecimal exchangeRate = resolvePaymentExchangeRate(purchaseOrder, request.getExchangeRate());

        Map<String, Object> before = paymentSnapshot(payment);
        UserEntity user = getUser(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        applyPaymentValues(
                payment, paymentType, installmentNo, request.getPaymentDate(),
                request.getAmount(), exchangeRate, request.getPaymentMethod(),
                request.getTransferReference(), request.getChequeBank(), request.getChequeNo(),
                request.getChequeDate(), request.getChequeBranch(), request.getRemark(), purchaseOrder.getCurrency()
        );
        assignSchedule(payment, schedule);
        payment.setUpdatedBy(user);
        payment.setUpdatedDate(now);
        addAttachments(payment, attachments, user, now, purchaseOrderNo);
        purchaseOrderPaymentRepository.saveAndFlush(payment);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", paymentSnapshot(payment));
        recordActivity(purchaseOrder, payment, userId, ActivityAction.UPDATE,
                "แก้ไขรายการชำระเงินของใบสั่งซื้อเลขที่ " + purchaseOrderNo, detail);
        return purchaseOrderPaymentMapper.toDto(payment);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentDto approvePayment(String purchaseOrderNo, Long paymentId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity purchaseOrder = getPurchaseOrderForUpdate(purchaseOrderNo);
        ensurePaymentAllowed(purchaseOrder);
        PurchaseOrderPaymentEntity payment = getPayment(purchaseOrderNo, paymentId);
        requireStatus(payment, PurchaseOrderPaymentStatus.PENDING);

        UserEntity user = getUser(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        payment.setStatus(PurchaseOrderPaymentStatus.APPROVED);
        payment.setApprovedBy(user);
        payment.setApprovedDate(now);
        payment.setUpdatedBy(user);
        payment.setUpdatedDate(now);
        recalculatePaymentSummary(purchaseOrder);
        purchaseOrder.setUpdatedBy(user);
        purchaseOrder.setUpdatedDate(now);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        recordActivity(purchaseOrder, payment, userId, ActivityAction.APPROVE,
                "อนุมัติรายการชำระเงินของใบสั่งซื้อเลขที่ " + purchaseOrderNo, null);
        return purchaseOrderPaymentMapper.toDto(payment);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentDto rejectPayment(
            String purchaseOrderNo,
            Long paymentId,
            String reason,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        String rejectionReason = StringUtils.trimToNull(reason);
        if (rejectionReason == null) {
            throw new InvalidRequestException("reason is required");
        }
        PurchaseOrderEntity purchaseOrder = getPurchaseOrderForUpdate(purchaseOrderNo);
        PurchaseOrderPaymentEntity payment = getPayment(purchaseOrderNo, paymentId);
        requireStatus(payment, PurchaseOrderPaymentStatus.PENDING);

        UserEntity user = getUser(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        payment.setStatus(PurchaseOrderPaymentStatus.REJECTED);
        payment.setRejectionReason(rejectionReason);
        payment.setRejectedBy(user);
        payment.setRejectedDate(now);
        payment.setUpdatedBy(user);
        payment.setUpdatedDate(now);
        purchaseOrderPaymentScheduleService.recalculateSchedules(purchaseOrder);
        purchaseOrder.setUpdatedBy(user);
        purchaseOrder.setUpdatedDate(now);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        recordActivity(purchaseOrder, payment, userId, ActivityAction.REJECT,
                "ไม่อนุมัติรายการชำระเงินของใบสั่งซื้อเลขที่ " + purchaseOrderNo,
                Map.of("reason", rejectionReason));
        return purchaseOrderPaymentMapper.toDto(payment);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentDto voidPayment(
            String purchaseOrderNo,
            Long paymentId,
            String reason,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        String voidReason = StringUtils.trimToNull(reason);
        if (voidReason == null) {
            throw new InvalidRequestException("reason is required");
        }
        PurchaseOrderEntity purchaseOrder = getPurchaseOrderForUpdate(purchaseOrderNo);
        PurchaseOrderPaymentEntity payment = getPayment(purchaseOrderNo, paymentId);
        requireStatus(payment, PurchaseOrderPaymentStatus.APPROVED);

        UserEntity user = getUser(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        payment.setStatus(PurchaseOrderPaymentStatus.VOIDED);
        payment.setVoidReason(voidReason);
        payment.setVoidedBy(user);
        payment.setVoidedDate(now);
        payment.setUpdatedBy(user);
        payment.setUpdatedDate(now);
        recalculatePaymentSummary(purchaseOrder);
        purchaseOrder.setUpdatedBy(user);
        purchaseOrder.setUpdatedDate(now);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        recordActivity(purchaseOrder, payment, userId, ActivityAction.UPDATE,
                "ยกเลิกรายการชำระเงินของใบสั่งซื้อเลขที่ " + purchaseOrderNo,
                Map.of("reason", voidReason));
        return purchaseOrderPaymentMapper.toDto(payment);
    }

    public void initializePaymentSummary(PurchaseOrderEntity purchaseOrder) {
        BigDecimal grandTotal = defaultAmount(purchaseOrder.getGrandTotal());
        BigDecimal grandTotalThb = defaultAmount(purchaseOrder.getGrandTotalThb());
        purchaseOrder.setPaymentStatus(PurchaseOrderPaymentLifecycleStatus.UNPAID);
        purchaseOrder.setPaidTotal(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setPaidTotalThb(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setOutstandingTotal(grandTotal.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setOutstandingTotalThb(grandTotalThb.setScale(5, RoundingMode.HALF_UP));
    }

    public void validateAndRecalculateAfterOrderTotalChange(PurchaseOrderEntity purchaseOrder)
            throws InvalidRequestException {
        BigDecimal committed = purchaseOrder.getPayments().stream()
                .filter(payment -> COMMITTED_STATUSES.contains(payment.getStatus()))
                .map(PurchaseOrderPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (committed.compareTo(defaultAmount(purchaseOrder.getGrandTotal())) > 0) {
            throw new InvalidRequestException("Purchase order total cannot be less than pending or approved payments");
        }
        purchaseOrderPaymentScheduleService.recalculateExpectedAmounts(purchaseOrder);
        for (PurchaseOrderPaymentScheduleEntity schedule : purchaseOrder.getPaymentSchedules()) {
            BigDecimal scheduleCommitted = schedule.getPayments().stream()
                    .filter(payment -> COMMITTED_STATUSES.contains(payment.getStatus()))
                    .map(PurchaseOrderPaymentEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (scheduleCommitted.compareTo(defaultAmount(schedule.getExpectedAmount())) > 0) {
                throw new InvalidRequestException(
                        "Purchase order total cannot make a payment schedule lower than its pending or approved payments"
                );
            }
        }
        recalculatePaymentSummary(purchaseOrder);
    }

    public void recalculatePaymentSummary(PurchaseOrderEntity purchaseOrder) {
        purchaseOrderPaymentScheduleService.recalculateSchedules(purchaseOrder);
        BigDecimal paidTotal = purchaseOrder.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                .map(PurchaseOrderPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidTotalThb = purchaseOrder.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                .map(PurchaseOrderPaymentEntity::getAmountThb)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = defaultAmount(purchaseOrder.getGrandTotal());
        BigDecimal grandTotalThb = defaultAmount(purchaseOrder.getGrandTotalThb());
        BigDecimal outstanding = grandTotal.subtract(paidTotal).max(BigDecimal.ZERO);
        BigDecimal outstandingThb = grandTotalThb.subtract(paidTotalThb).max(BigDecimal.ZERO);

        purchaseOrder.setPaidTotal(paidTotal.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setPaidTotalThb(paidTotalThb.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setOutstandingTotal(outstanding.setScale(5, RoundingMode.HALF_UP));
        purchaseOrder.setOutstandingTotalThb(outstandingThb.setScale(5, RoundingMode.HALF_UP));

        if (paidTotal.compareTo(BigDecimal.ZERO) <= 0) {
            purchaseOrder.setPaymentStatus(PurchaseOrderPaymentLifecycleStatus.UNPAID);
        } else if (paidTotal.compareTo(grandTotal) >= 0) {
            purchaseOrder.setPaymentStatus(PurchaseOrderPaymentLifecycleStatus.PAID);
        } else {
            purchaseOrder.setPaymentStatus(PurchaseOrderPaymentLifecycleStatus.PARTIALLY_PAID);
        }

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.CANCELLED
                && purchaseOrder.getStatus() != PurchaseOrderStatus.CLOSED) {
            purchaseOrder.setStatus(purchaseOrder.getPaymentStatus() == PurchaseOrderPaymentLifecycleStatus.PAID
                    ? PurchaseOrderStatus.PAID
                    : PurchaseOrderStatus.AWAITING_PAYMENT);
        }
    }

    private PurchaseOrderEntity getPurchaseOrderForUpdate(String purchaseOrderNo) throws DataNotFoundException {
        return purchaseOrderRepository.findByIdForUpdate(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
    }

    private PurchaseOrderPaymentEntity getPayment(String purchaseOrderNo, Long paymentId) throws DataNotFoundException {
        return purchaseOrderPaymentRepository.findByIdAndPurchaseOrderPurchaseOrderNo(paymentId, purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order payment " + paymentId + " not found."));
    }

    private UserEntity getUser(String userId) throws DataNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
    }

    private void ensurePaymentAllowed(PurchaseOrderEntity purchaseOrder) throws InvalidRequestException {
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidRequestException("Cancelled purchase order cannot receive payments");
        }
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Closed purchase order cannot receive payments");
        }
    }

    private void requireStatus(PurchaseOrderPaymentEntity payment, PurchaseOrderPaymentStatus expected)
            throws InvalidRequestException {
        if (payment.getStatus() != expected) {
            throw new InvalidRequestException("Purchase order payment status must be " + expected.name());
        }
    }

    private void validatePaymentRequest(
            PurchaseOrderPaymentType paymentType,
            Integer installmentNo,
            ZonedDateTime paymentDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String chequeBank,
            String chequeNo,
            java.time.LocalDate chequeDate,
            List<MultipartFile> attachments,
            boolean isCreate
    ) throws InvalidRequestException {
        if (paymentType == null) throw new InvalidRequestException("paymentType is required");
        if (paymentType == PurchaseOrderPaymentType.INSTALLMENT && (installmentNo == null || installmentNo <= 0)) {
            throw new InvalidRequestException("installmentNo must be greater than zero for installment payments");
        }
        if (paymentDate == null) throw new InvalidRequestException("paymentDate is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }
        if (paymentMethod == null) throw new InvalidRequestException("paymentMethod is required");
        if (paymentMethod == PaymentMethod.TRANSFER) {
            boolean hasAttachment = attachments != null && attachments.stream().anyMatch(file -> file != null && !file.isEmpty());
            if (isCreate && !hasAttachment) {
                throw new InvalidRequestException("At least one payment slip is required for transfer payments");
            }
        }
        if (paymentMethod == PaymentMethod.CHEQUE) {
            if (StringUtils.isBlank(chequeBank)) throw new InvalidRequestException("chequeBank is required for cheque payments");
            if (StringUtils.isBlank(chequeNo)) throw new InvalidRequestException("chequeNo is required for cheque payments");
            if (chequeDate == null) throw new InvalidRequestException("chequeDate is required for cheque payments");
        }
    }

    BigDecimal resolvePaymentExchangeRate(
            PurchaseOrderEntity purchaseOrder,
            BigDecimal requestedExchangeRate
    ) {
        if (purchaseOrder.getCurrency() == Currency.THB) {
            return BigDecimal.ONE;
        }
        if (requestedExchangeRate != null && requestedExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            return requestedExchangeRate;
        }

        BigDecimal grandTotal = defaultAmount(purchaseOrder.getGrandTotal());
        BigDecimal grandTotalThb = defaultAmount(purchaseOrder.getGrandTotalThb());
        if (grandTotal.compareTo(BigDecimal.ZERO) > 0 && grandTotalThb.compareTo(BigDecimal.ZERO) > 0) {
            return grandTotalThb.divide(grandTotal, 6, RoundingMode.HALF_UP);
        }

        return BigDecimal.ONE;
    }

    private void validateCommittedLimit(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderPaymentScheduleEntity schedule,
            BigDecimal requestedAmount,
            Long excludedPaymentId
    )
            throws InvalidRequestException {
        BigDecimal committed = purchaseOrder.getPayments().stream()
                .filter(payment -> excludedPaymentId == null || !Objects.equals(payment.getId(), excludedPaymentId))
                .filter(payment -> COMMITTED_STATUSES.contains(payment.getStatus()))
                .map(PurchaseOrderPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (committed.add(requestedAmount).compareTo(defaultAmount(purchaseOrder.getGrandTotal())) > 0) {
            throw new InvalidRequestException("Payment amount exceeds the remaining purchase order total");
        }
        if (schedule != null) {
            BigDecimal scheduleCommitted = schedule.getPayments().stream()
                    .filter(payment -> excludedPaymentId == null || !Objects.equals(payment.getId(), excludedPaymentId))
                    .filter(payment -> COMMITTED_STATUSES.contains(payment.getStatus()))
                    .map(PurchaseOrderPaymentEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (scheduleCommitted.add(requestedAmount)
                    .compareTo(defaultAmount(schedule.getExpectedAmount())) > 0) {
                throw new InvalidRequestException("Payment amount exceeds the remaining payment schedule amount");
            }
        }
    }

    private PurchaseOrderPaymentScheduleEntity resolveSchedule(PurchaseOrderEntity purchaseOrder, Long scheduleId)
            throws InvalidRequestException {
        if (scheduleId == null) {
            if (!purchaseOrder.getPaymentSchedules().isEmpty()) {
                throw new InvalidRequestException("scheduleId is required");
            }
            return null;
        }
        return purchaseOrder.getPaymentSchedules().stream()
                .filter(schedule -> Objects.equals(schedule.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Payment schedule " + scheduleId + " does not belong to this purchase order"
                ));
    }

    private void assignSchedule(
            PurchaseOrderPaymentEntity payment,
            PurchaseOrderPaymentScheduleEntity schedule
    ) {
        PurchaseOrderPaymentScheduleEntity previous = payment.getSchedule();
        if (previous == schedule) return;
        if (previous != null) previous.removePayment(payment);
        if (schedule != null) schedule.addPayment(payment);
    }

    private void applyPaymentValues(
            PurchaseOrderPaymentEntity payment,
            PurchaseOrderPaymentType paymentType,
            Integer installmentNo,
            ZonedDateTime paymentDate,
            BigDecimal amount,
            BigDecimal exchangeRate,
            PaymentMethod paymentMethod,
            String transferReference,
            String chequeBank,
            String chequeNo,
            java.time.LocalDate chequeDate,
            String chequeBranch,
            String remark,
            Currency currency
    ) {
        BigDecimal normalizedRate = currency == Currency.THB ? BigDecimal.ONE : exchangeRate;
        payment.setPaymentType(paymentType);
        payment.setInstallmentNo(paymentType == PurchaseOrderPaymentType.INSTALLMENT ? installmentNo : null);
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount.setScale(5, RoundingMode.HALF_UP));
        payment.setCurrency(currency);
        payment.setExchangeRate(normalizedRate.setScale(6, RoundingMode.HALF_UP));
        payment.setAmountThb(amount.multiply(normalizedRate).setScale(5, RoundingMode.HALF_UP));
        payment.setPaymentMethod(paymentMethod);
        payment.setTransferReference(paymentMethod == PaymentMethod.TRANSFER ? StringUtils.trimToNull(transferReference) : null);
        payment.setChequeBank(paymentMethod == PaymentMethod.CHEQUE ? StringUtils.trimToNull(chequeBank) : null);
        payment.setChequeNo(paymentMethod == PaymentMethod.CHEQUE ? StringUtils.trimToNull(chequeNo) : null);
        payment.setChequeDate(paymentMethod == PaymentMethod.CHEQUE ? chequeDate : null);
        payment.setChequeBranch(paymentMethod == PaymentMethod.CHEQUE ? StringUtils.trimToNull(chequeBranch) : null);
        payment.setRemark(StringUtils.trimToNull(remark));
    }

    private void addAttachments(
            PurchaseOrderPaymentEntity payment,
            List<MultipartFile> attachments,
            UserEntity user,
            ZonedDateTime now,
            String purchaseOrderNo
    ) throws Exception {
        if (attachments == null) return;
        PurchaseOrderEntity purchaseOrder = payment.getPurchaseOrder();
        int sortOrder = purchaseOrder.getAttachments().stream()
                .filter(attachment -> Boolean.TRUE.equals(attachment.getActive()))
                .map(PurchaseOrderAttachmentEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) continue;
            UploadFileResponse uploaded = fileStorageService.uploadFile(file, "purchase-orders/" + purchaseOrderNo + "/payments");
            PurchaseOrderAttachmentEntity attachment = new PurchaseOrderAttachmentEntity();
            attachment.setDocumentType(PurchaseOrderAttachmentDocumentType.PAYMENT_SLIP);
            attachment.setFileName(uploaded.getFileName());
            attachment.setOriginalFileName(StringUtils.trimToNull(file.getOriginalFilename()));
            attachment.setFileUrl(uploaded.getUrl());
            attachment.setContentType(StringUtils.trimToNull(uploaded.getContentType()));
            attachment.setFileSize(file.getSize());
            attachment.setSortOrder(sortOrder++);
            attachment.setActive(Boolean.TRUE);
            attachment.setCreatedBy(user);
            attachment.setUpdatedBy(user);
            attachment.setCreatedDate(now);
            attachment.setUpdatedDate(now);
            purchaseOrder.addAttachment(attachment);
            payment.addAttachment(attachment);
        }
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> paymentSnapshot(PurchaseOrderPaymentEntity payment) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("paymentId", payment.getId());
        detail.put("scheduleId", payment.getSchedule() == null ? null : payment.getSchedule().getId());
        detail.put("paymentType", payment.getPaymentType());
        detail.put("installmentNo", payment.getInstallmentNo());
        detail.put("paymentDate", payment.getPaymentDate());
        detail.put("amount", payment.getAmount());
        detail.put("currency", payment.getCurrency());
        detail.put("exchangeRate", payment.getExchangeRate());
        detail.put("amountThb", payment.getAmountThb());
        detail.put("paymentMethod", payment.getPaymentMethod());
        detail.put("status", payment.getStatus());
        return detail;
    }

    private void recordActivity(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderPaymentEntity payment,
            String userId,
            ActivityAction action,
            String summary,
            Map<String, Object> extra
    ) {
        Map<String, Object> detail = new LinkedHashMap<>(paymentSnapshot(payment));
        detail.put("paymentStatus", purchaseOrder.getPaymentStatus());
        detail.put("paidTotal", purchaseOrder.getPaidTotal());
        detail.put("outstandingTotal", purchaseOrder.getOutstandingTotal());
        if (extra != null) detail.putAll(extra);
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                purchaseOrder.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                action,
                ActivitySource.WEB,
                summary,
                detail
        );
    }
}
