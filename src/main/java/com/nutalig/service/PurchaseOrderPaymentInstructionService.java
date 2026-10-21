package com.nutalig.service;

import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import com.nutalig.constant.Currency;
import com.nutalig.constant.Effect;
import com.nutalig.constant.PurchaseOrderPaymentInstructionStatus;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.Status;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderPaymentInstructionRequest;
import com.nutalig.dto.PurchaseOrderPaymentInstructionDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.entity.PurchaseOrderPaymentInstructionEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.entity.RolePermissionEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.entity.UserPermissionOverrideEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderPaymentInstructionRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.repository.RolePermissionRepository;
import com.nutalig.repository.UserPermissionOverrideRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderPaymentInstructionService {

    private static final String APPROVE_PERMISSION = "PERM_PURCHASE_ORDER_PAYMENT_APPROVE";
    private static final Set<PurchaseOrderPaymentStatus> COMMITTED_PAYMENT_STATUSES = EnumSet.of(
            PurchaseOrderPaymentStatus.PENDING,
            PurchaseOrderPaymentStatus.APPROVED
    );
    private static final Set<PurchaseOrderPaymentInstructionStatus> ACTIVE_INSTRUCTION_STATUSES = EnumSet.of(
            PurchaseOrderPaymentInstructionStatus.DRAFT,
            PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT,
            PurchaseOrderPaymentInstructionStatus.PARTIALLY_PAID
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderPaymentInstructionRepository instructionRepository;
    private final PurchaseOrderPaymentScheduleService paymentScheduleService;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;
    private final UserMapper userMapper;
    private final LineMessageService lineMessageService;
    private final LineConfiguration lineConfiguration;
    private final ActivityHistoryService activityHistoryService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderPaymentInstructionDto> getInstructions(String purchaseOrderNo)
            throws DataNotFoundException {
        if (!purchaseOrderRepository.existsById(purchaseOrderNo)) {
            throw new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found.");
        }
        return instructionRepository
                .findBySchedule_PurchaseOrder_PurchaseOrderNoOrderByCreatedDateDescIdDesc(purchaseOrderNo)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto createInstruction(
            String purchaseOrderNo,
            CreatePurchaseOrderPaymentInstructionRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        if (request == null || request.getScheduleId() == null) {
            throw new InvalidRequestException("scheduleId is required");
        }
        if (request.getAmountThb() == null || request.getAmountThb().signum() <= 0) {
            throw new InvalidRequestException("amountThb must be greater than zero");
        }
        if (request.getPlannedPaymentDate() == null) {
            throw new InvalidRequestException("plannedPaymentDate is required");
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderPaymentScheduleEntity schedule = purchaseOrder.getPaymentSchedules().stream()
                .filter(item -> Objects.equals(item.getId(), request.getScheduleId()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Payment schedule " + request.getScheduleId() + " does not belong to this purchase order"));
        if (purchaseOrder.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CANCELLED
                || purchaseOrder.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Cannot create a payment instruction for a closed purchase order");
        }

        Currency currency = purchaseOrder.getCurrency() == null ? Currency.THB : purchaseOrder.getCurrency();
        BigDecimal rate = validateRate(request.getExchangeRate());
        BigDecimal amountThb = request.getAmountThb().setScale(5, RoundingMode.HALF_UP);
        BigDecimal amount = requestedAmount(request).setScale(5, RoundingMode.HALF_UP);
        validateRemainingCapacity(purchaseOrder, schedule, amount, null);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        ZonedDateTime now = ZonedDateTime.now();
        PurchaseOrderPaymentInstructionEntity instruction = new PurchaseOrderPaymentInstructionEntity();
        instruction.setSchedule(schedule);
        instruction.setAmount(amount);
        instruction.setAmountThb(amountThb);
        instruction.setCurrency(currency);
        instruction.setExchangeRate(rate == null ? null : rate.setScale(6, RoundingMode.HALF_UP));
        instruction.setPlannedPaymentDate(request.getPlannedPaymentDate());
        instruction.setPaidAmount(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
        instruction.setPaidAmountThb(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
        instruction.setStatus(PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT);
        instruction.setRemark(StringUtils.trimToNull(request.getRemark()));
        instruction.setCreatedBy(user);
        instruction.setUpdatedBy(user);
        instruction.setCreatedDate(now);
        instruction.setUpdatedDate(now);
        instruction = instructionRepository.saveAndFlush(instruction);
        paymentScheduleService.recalculateSchedules(purchaseOrder);
        recordInstructionActivity(purchaseOrder, instruction, userId, ActivityAction.CREATE,
                "สร้างรายการตั้งจ่าย PO " + purchaseOrderNo, null, instructionSnapshot(instruction));
        notifyApproversAfterCommit(instruction.getId());
        return toDto(instruction);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto createDraft(String purchaseOrderNo,
            CreatePurchaseOrderPaymentInstructionRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null || request.getScheduleId() == null || request.getAmount() == null
                || request.getAmount().signum() <= 0 || request.getPlannedPaymentDate() == null) {
            throw new InvalidRequestException("scheduleId, positive amount and plannedPaymentDate are required");
        }
        PurchaseOrderEntity po = purchaseOrderRepository.findByIdForUpdate(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderPaymentScheduleEntity schedule = po.getPaymentSchedules().stream()
                .filter(s -> Objects.equals(s.getId(), request.getScheduleId())).findFirst()
                .orElseThrow(() -> new InvalidRequestException("Payment schedule does not belong to this purchase order"));
        if (po.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CANCELLED
                || po.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Cannot create a payment instruction for a closed purchase order");
        }
        BigDecimal amount = request.getAmount().setScale(5, RoundingMode.HALF_UP);
        validateRemainingCapacity(po, schedule, amount, null);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        ZonedDateTime now = ZonedDateTime.now();
        PurchaseOrderPaymentInstructionEntity i = new PurchaseOrderPaymentInstructionEntity();
        i.setSchedule(schedule); i.setAmount(amount); i.setAmountThb(null);
        i.setCurrency(po.getCurrency() == null ? Currency.THB : po.getCurrency());
        i.setExchangeRate(null); i.setPlannedPaymentDate(request.getPlannedPaymentDate());
        i.setPaidAmount(BigDecimal.ZERO.setScale(5)); i.setPaidAmountThb(BigDecimal.ZERO.setScale(5));
        i.setStatus(PurchaseOrderPaymentInstructionStatus.DRAFT); i.setRemark(StringUtils.trimToNull(request.getRemark()));
        i.setCreatedBy(user); i.setUpdatedBy(user); i.setCreatedDate(now); i.setUpdatedDate(now);
        i = instructionRepository.saveAndFlush(i);
        recordInstructionActivity(po, i, userId, ActivityAction.CREATE,
                "สร้างร่างรายการตั้งจ่าย PO " + purchaseOrderNo, null, instructionSnapshot(i));
        notifyAccountsAfterCommit(i.getId());
        return toDto(i);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto submitDraft(String poNo, Long id,
            CreatePurchaseOrderPaymentInstructionRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderPaymentEntityHolder holder = loadInstruction(poNo, id);
        PurchaseOrderPaymentInstructionEntity i = holder.instruction();
        Map<String, Object> before = instructionSnapshot(i);
        if (i.getStatus() != PurchaseOrderPaymentInstructionStatus.DRAFT)
            throw new InvalidRequestException("Only draft payment instructions can be submitted");
        if (holder.purchaseOrder().getStatus() == com.nutalig.constant.PurchaseOrderStatus.CANCELLED
                || holder.purchaseOrder().getStatus() == com.nutalig.constant.PurchaseOrderStatus.CLOSED)
            throw new InvalidRequestException("Cannot submit a payment instruction for a closed purchase order");
        if (request == null || request.getAmountThb() == null || request.getAmountThb().signum() <= 0
                || request.getPlannedPaymentDate() == null)
            throw new InvalidRequestException("positive amountThb and plannedPaymentDate are required");
        if (request.getAmount() != null && request.getAmount().signum() <= 0)
            throw new InvalidRequestException("amount must be greater than zero");
        BigDecimal rate = validateRate(request.getExchangeRate());
        if (request.getAmount() != null) {
            validateRemainingCapacity(holder.purchaseOrder(), i.getSchedule(), request.getAmount().setScale(5, RoundingMode.HALF_UP), id);
            i.setAmount(request.getAmount().setScale(5, RoundingMode.HALF_UP));
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        i.setAmountThb(request.getAmountThb().setScale(5, RoundingMode.HALF_UP));
        i.setExchangeRate(rate == null ? null : rate.setScale(6, RoundingMode.HALF_UP));
        i.setPlannedPaymentDate(request.getPlannedPaymentDate());
        i.setRemark(StringUtils.trimToNull(request.getRemark()));
        i.setStatus(PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT);
        i.setUpdatedBy(user); i.setUpdatedDate(ZonedDateTime.now());
        i = instructionRepository.saveAndFlush(i);
        paymentScheduleService.recalculateSchedules(holder.purchaseOrder());
        recordInstructionActivity(holder.purchaseOrder(), i, userId, ActivityAction.STATUS_CHANGE,
                "ส่งรายการตั้งจ่าย PO " + poNo + " ให้ผู้อนุมัติ", before, instructionSnapshot(i));
        notifyApproversAfterCommit(i.getId());
        return toDto(i);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto updateInstruction(
            String purchaseOrderNo,
            Long instructionId,
            CreatePurchaseOrderPaymentInstructionRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        if (request == null || request.getScheduleId() == null || request.getAmountThb() == null
                || request.getAmountThb().signum() <= 0 || request.getPlannedPaymentDate() == null) {
            throw new InvalidRequestException("scheduleId, positive amountThb and plannedPaymentDate are required");
        }
        PurchaseOrderPaymentEntityHolder holder = loadInstruction(purchaseOrderNo, instructionId);
        PurchaseOrderPaymentInstructionEntity instruction = holder.instruction();
        if (!ACTIVE_INSTRUCTION_STATUSES.contains(instruction.getStatus()) || instruction.getPayments().stream()
                .anyMatch(payment -> COMMITTED_PAYMENT_STATUSES.contains(payment.getStatus()))) {
            throw new InvalidRequestException("Only unpaid payment instructions can be edited");
        }
        PurchaseOrderEntity purchaseOrder = holder.purchaseOrder();
        Map<String, Object> before = instructionSnapshot(instruction);
        if (purchaseOrder.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CANCELLED
                || purchaseOrder.getStatus() == com.nutalig.constant.PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Cannot edit a payment instruction for a closed purchase order");
        }
        PurchaseOrderPaymentScheduleEntity schedule = purchaseOrder.getPaymentSchedules().stream()
                .filter(item -> Objects.equals(item.getId(), request.getScheduleId()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Payment schedule " + request.getScheduleId() + " does not belong to this purchase order"));
        Currency currency = purchaseOrder.getCurrency() == null ? Currency.THB : purchaseOrder.getCurrency();
        BigDecimal rate = validateRate(request.getExchangeRate());
        BigDecimal amountThb = request.getAmountThb().setScale(5, RoundingMode.HALF_UP);
        BigDecimal amount = requestedAmount(request).setScale(5, RoundingMode.HALF_UP);
        validateRemainingCapacity(purchaseOrder, schedule, amount, instructionId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        instruction.setSchedule(schedule);
        instruction.setAmount(amount);
        instruction.setAmountThb(amountThb);
        instruction.setCurrency(currency);
        instruction.setExchangeRate(rate == null ? null : rate.setScale(6, RoundingMode.HALF_UP));
        instruction.setPlannedPaymentDate(request.getPlannedPaymentDate());
        instruction.setRemark(StringUtils.trimToNull(request.getRemark()));
        instruction.setUpdatedBy(user);
        instruction.setUpdatedDate(ZonedDateTime.now());
        instruction = instructionRepository.saveAndFlush(instruction);
        paymentScheduleService.recalculateSchedules(purchaseOrder);
        recordInstructionActivity(purchaseOrder, instruction, userId, ActivityAction.UPDATE,
                "แก้ไขรายการตั้งจ่าย PO " + purchaseOrderNo, before, instructionSnapshot(instruction));
        notifyApproversAfterCommit(instruction.getId());
        return toDto(instruction);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto cancelInstruction(String purchaseOrderNo, Long instructionId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderPaymentInstructionEntity instruction = instructionRepository
                .findByIdForUpdate(instructionId, purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException(
                        "Payment instruction " + instructionId + " not found for purchase order " + purchaseOrderNo));
        if (!ACTIVE_INSTRUCTION_STATUSES.contains(instruction.getStatus())) {
            throw new InvalidRequestException("Only outstanding payment instructions can be cancelled");
        }
        Map<String, Object> before = instructionSnapshot(instruction);
        boolean hasCommittedPayments = instruction.getPayments().stream()
                .anyMatch(payment -> COMMITTED_PAYMENT_STATUSES.contains(payment.getStatus()));
        if (hasCommittedPayments) {
            throw new InvalidRequestException("Payment instruction with recorded payments cannot be cancelled");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        instruction.setStatus(PurchaseOrderPaymentInstructionStatus.CANCELLED);
        instruction.setUpdatedBy(user);
        instruction.setUpdatedDate(ZonedDateTime.now());
        instruction = instructionRepository.saveAndFlush(instruction);
        PurchaseOrderEntity purchaseOrder = instruction.getSchedule().getPurchaseOrder();
        paymentScheduleService.recalculateSchedules(purchaseOrder);
        recordInstructionActivity(purchaseOrder, instruction, userId, ActivityAction.STATUS_CHANGE,
                "ยกเลิกรายการตั้งจ่าย PO " + purchaseOrderNo, before, instructionSnapshot(instruction));
        return toDto(instruction);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderPaymentInstructionDto confirmInstructionPaid(
            String purchaseOrderNo,
            Long instructionId,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderPaymentEntityHolder holder = loadInstruction(purchaseOrderNo, instructionId);
        PurchaseOrderPaymentInstructionEntity instruction = holder.instruction();
        Map<String, Object> before = instructionSnapshot(instruction);
        if (instruction.getStatus() != PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT
                || instruction.getPaidAmount() == null
                || instruction.getPaidAmount().signum() > 0
                || instruction.getPayments().stream()
                        .anyMatch(payment -> COMMITTED_PAYMENT_STATUSES.contains(payment.getStatus()))) {
            throw new InvalidRequestException("Only unpaid payment instructions can be confirmed as paid");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        instruction.setPaidAmount(defaultAmount(instruction.getAmount()).setScale(5, RoundingMode.HALF_UP));
        instruction.setPaidAmountThb(defaultAmount(instruction.getAmountThb()).setScale(5, RoundingMode.HALF_UP));
        instruction.setManuallyConfirmedPaid(true);
        instruction.setStatus(PurchaseOrderPaymentInstructionStatus.PAID);
        instruction.setUpdatedBy(user);
        ZonedDateTime confirmedPaidDate = ZonedDateTime.now();
        instruction.setConfirmedPaidDate(confirmedPaidDate);
        instruction.setUpdatedDate(confirmedPaidDate);
        instruction = instructionRepository.saveAndFlush(instruction);
        recordInstructionActivity(holder.purchaseOrder(), instruction, userId, ActivityAction.STATUS_CHANGE,
                "ยืนยันชำระรายการตั้งจ่าย PO " + purchaseOrderNo, before, instructionSnapshot(instruction));
        return toDto(instruction);
    }

    private Map<String, Object> instructionSnapshot(PurchaseOrderPaymentInstructionEntity instruction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("instructionId", instruction.getId());
        snapshot.put("scheduleId", instruction.getSchedule().getId());
        snapshot.put("installmentNo", instruction.getSchedule().getInstallmentNo());
        snapshot.put("amount", instruction.getAmount());
        snapshot.put("currency", instruction.getCurrency());
        snapshot.put("amountThb", instruction.getAmountThb());
        snapshot.put("exchangeRate", instruction.getExchangeRate());
        snapshot.put("plannedPaymentDate", instruction.getPlannedPaymentDate());
        snapshot.put("status", instruction.getStatus());
        snapshot.put("paidAmount", instruction.getPaidAmount());
        snapshot.put("paidAmountThb", instruction.getPaidAmountThb());
        snapshot.put("remark", instruction.getRemark());
        return snapshot;
    }

    private void recordInstructionActivity(PurchaseOrderEntity purchaseOrder,
            PurchaseOrderPaymentInstructionEntity instruction, String userId, ActivityAction action,
            String summary, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (before != null) detail.put("before", before);
        if (after != null) detail.put("after", after);
        detail.put("instructionId", instruction.getId());
        detail.put("scheduleId", instruction.getSchedule().getId());
        detail.put("installmentNo", instruction.getSchedule().getInstallmentNo());
        activityHistoryService.record(ActivityEntityType.PURCHASE_ORDER,
                purchaseOrder.getPurchaseOrderNo(), userId, ActivityActorType.USER,
                action, ActivitySource.WEB, summary, detail);
    }

    private BigDecimal requestedAmount(CreatePurchaseOrderPaymentInstructionRequest request)
            throws InvalidRequestException {
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : request.getAmountThb();
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }
        return amount;
    }

    private BigDecimal validateRate(BigDecimal requestedRate) throws InvalidRequestException {
        if (requestedRate != null && requestedRate.signum() <= 0) {
            throw new InvalidRequestException("exchangeRate must be greater than zero when provided");
        }
        return requestedRate;
    }

    public void recalculateInstruction(PurchaseOrderPaymentInstructionEntity instruction) {
        if (instruction.isManuallyConfirmedPaid()) return;
        BigDecimal paid = instruction.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                .map(PurchaseOrderPaymentEntity::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidThb = instruction.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                .map(PurchaseOrderPaymentEntity::getAmountThb).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        instruction.setPaidAmount(paid.setScale(5, RoundingMode.HALF_UP));
        instruction.setPaidAmountThb(paidThb.setScale(5, RoundingMode.HALF_UP));
        if (instruction.getStatus() == PurchaseOrderPaymentInstructionStatus.CANCELLED) return;
        if (paid.compareTo(instruction.getAmount()) >= 0) {
            instruction.setStatus(PurchaseOrderPaymentInstructionStatus.PAID);
        } else if (paid.signum() > 0) {
            instruction.setStatus(PurchaseOrderPaymentInstructionStatus.PARTIALLY_PAID);
        } else {
            instruction.setStatus(PurchaseOrderPaymentInstructionStatus.AWAITING_PAYMENT);
        }
    }

    private void validateRemainingCapacity(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderPaymentScheduleEntity schedule,
            BigDecimal requestedAmount,
            Long excludedInstructionId
    ) throws InvalidRequestException {
        BigDecimal poCommitted = committedPaymentAmount(purchaseOrder.getPayments())
                .add(reservedInstructionAmount(instructionRepository
                        .findBySchedule_PurchaseOrder_PurchaseOrderNoOrderByCreatedDateDescIdDesc(
                                purchaseOrder.getPurchaseOrderNo()), excludedInstructionId));
        BigDecimal scheduleCommitted = committedPaymentAmount(schedule.getPayments())
                .add(reservedInstructionAmount(instructionRepository.findBySchedule_IdIn(List.of(schedule.getId())),
                        excludedInstructionId));
        if (poCommitted.add(requestedAmount).compareTo(defaultAmount(purchaseOrder.getGrandTotal())) > 0) {
            throw new InvalidRequestException("Payment instruction exceeds the remaining purchase order total");
        }
        if (scheduleCommitted.add(requestedAmount).compareTo(defaultAmount(schedule.getExpectedAmount())) > 0) {
            throw new InvalidRequestException("Payment instruction exceeds the remaining payment schedule amount");
        }
    }

    private BigDecimal committedPaymentAmount(Collection<PurchaseOrderPaymentEntity> payments) {
        return payments.stream().filter(payment -> COMMITTED_PAYMENT_STATUSES.contains(payment.getStatus()))
                .map(PurchaseOrderPaymentEntity::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal reservedInstructionAmount(
            Collection<PurchaseOrderPaymentInstructionEntity> instructions,
            Long excludedInstructionId
    ) {
        return instructions.stream().filter(instruction -> ACTIVE_INSTRUCTION_STATUSES.contains(instruction.getStatus()))
                .filter(instruction -> excludedInstructionId == null || !Objects.equals(instruction.getId(), excludedInstructionId))
                .map(instruction -> defaultAmount(instruction.getAmount()).subtract(defaultAmount(instruction.getPaidAmount()))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseOrderPaymentEntityHolder loadInstruction(String purchaseOrderNo, Long instructionId)
            throws DataNotFoundException {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderPaymentInstructionEntity instruction = instructionRepository
                .findByIdForUpdate(instructionId, purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException(
                        "Payment instruction " + instructionId + " not found for purchase order " + purchaseOrderNo));
        return new PurchaseOrderPaymentEntityHolder(purchaseOrder, instruction);
    }

    private record PurchaseOrderPaymentEntityHolder(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderPaymentInstructionEntity instruction
    ) {}

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void notifyApproversAfterCommit(Long instructionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notifyApprovers(instructionId);
                } catch (Exception exception) {
                    log.warn("Cannot notify approvers for payment instruction {}", instructionId, exception);
                }
            }
        });
    }

    private void notifyAccountsAfterCommit(Long id) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { notifyAccounts(id); } catch (Exception e) { log.warn("Cannot notify accounts for draft {}", id, e); }
            }
        });
    }

    @Transactional(readOnly = true)
    public void notifyAccounts(Long id) throws Exception {
        PurchaseOrderPaymentInstructionEntity i = instructionRepository.findWithNotificationDetailsById(id)
                .orElseThrow(() -> new DataNotFoundException("Payment instruction " + id + " not found."));
        Set<String> roles = rolePermissionRepository.findByPermissionCode("PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT")
                .stream().map(RolePermissionEntity::getRoleCode).collect(Collectors.toSet());
        Map<String, Effect> overrides = userPermissionOverrideRepository.findByPermissionCode("PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT")
                .stream().collect(Collectors.toMap(UserPermissionOverrideEntity::getUserId, UserPermissionOverrideEntity::getEffect, (a,b)->a));
        URI login = URI.create(lineConfiguration.getLoginSuccessUrl());
        PurchaseOrderEntity po = i.getSchedule().getPurchaseOrder();
        String url = UriComponentsBuilder.fromUriString(login.getScheme()+"://"+login.getAuthority())
                .pathSegment("purchase-order", po.getPurchaseOrderNo()).queryParam("tab", "payment").build().toUriString();
        for (UserEntity u : userRepository.findAllWithRole()) {
            Effect override = overrides.get(u.getId());
            boolean allowed = override == Effect.ALLOW || (override != Effect.DENY && u.getUserRoleEntity()!=null
                    && roles.contains(u.getUserRoleEntity().getRoleCode()));
            if (allowed && u.getStatus()==Status.ACTIVE && StringUtils.isNotBlank(u.getLineUserId()))
                lineMessageService.sendNotificationCard(u.getId(), "มีรายการตั้งจ่ายรอตรวจ PO "+po.getPurchaseOrderNo(),
                        "รายการตั้งจ่ายรอตรวจสอบ", "งวดที่ "+i.getSchedule().getInstallmentNo()+" ยอด "+i.getAmount()+" "+i.getCurrency(), url);
        }
    }

    @Transactional(readOnly = true)
    public void notifyApprovers(Long instructionId) throws Exception {
        PurchaseOrderPaymentInstructionEntity instruction = instructionRepository.findWithNotificationDetailsById(instructionId)
                .orElseThrow(() -> new DataNotFoundException("Payment instruction " + instructionId + " not found."));
        Set<String> rolesWithPermission = rolePermissionRepository.findByPermissionCode(APPROVE_PERMISSION).stream()
                .map(RolePermissionEntity::getRoleCode).collect(Collectors.toSet());
        Map<String, Effect> overrides = userPermissionOverrideRepository.findByPermissionCode(APPROVE_PERMISSION).stream()
                .collect(Collectors.toMap(UserPermissionOverrideEntity::getUserId,
                        UserPermissionOverrideEntity::getEffect, (first, ignored) -> first));
        List<UserEntity> recipients = userRepository.findAllWithRole().stream()
                .filter(user -> user.getStatus() == Status.ACTIVE && StringUtils.isNotBlank(user.getLineUserId()))
                .filter(user -> {
                    Effect override = overrides.get(user.getId());
                    if (override == Effect.ALLOW) return true;
                    if (override == Effect.DENY) return false;
                    return user.getUserRoleEntity() != null
                            && rolesWithPermission.contains(user.getUserRoleEntity().getRoleCode());
                })
                .toList();
        if (recipients.isEmpty()) {
            log.warn("No active payment approvers with LINE binding for payment instruction {}", instructionId);
            return;
        }
        PurchaseOrderPaymentScheduleEntity schedule = instruction.getSchedule();
        PurchaseOrderEntity purchaseOrder = schedule.getPurchaseOrder();
        String salesOrderNo = purchaseOrder.getSalesOrder() == null
                ? "-" : purchaseOrder.getSalesOrder().getSalesOrderNo();
        String supplierName = StringUtils.defaultIfBlank(purchaseOrder.getSupplierNameSnapshot(),
                purchaseOrder.getSupplier() == null ? "-" : purchaseOrder.getSupplier().getSupplierName());
        URI loginUri = URI.create(lineConfiguration.getLoginSuccessUrl());
        String detailUrl = UriComponentsBuilder.fromUriString(loginUri.getScheme() + "://" + loginUri.getAuthority())
                .pathSegment("purchase-order", purchaseOrder.getPurchaseOrderNo())
                .queryParam("tab", "payment")
                .build().toUriString();
        String detail = "เลขที่ PO: " + purchaseOrder.getPurchaseOrderNo()
                + "\nเลขที่ SO: " + salesOrderNo
                + "\nSupplier: " + supplierName
                + "\nงวดที่ " + schedule.getInstallmentNo()
                + "\nยอดตั้งจ่าย: " + instruction.getAmountThb().stripTrailingZeros().toPlainString() + " THB"
                + "\nยอดเทียบ PO: " + instruction.getAmount().stripTrailingZeros().toPlainString()
                + " " + instruction.getCurrency().name()
                + "\nกำหนดจ่าย: " + instruction.getPlannedPaymentDate();
        for (UserEntity recipient : recipients) {
            try {
                lineMessageService.sendNotificationCard(recipient.getId(),
                        "มีรายการตั้งจ่าย PO " + purchaseOrder.getPurchaseOrderNo(),
                        "PO รอดำเนินการจ่าย", detail, detailUrl);
            } catch (Exception exception) {
                log.warn("Cannot notify payment approver {} for instruction {}", recipient.getId(), instructionId,
                        exception);
            }
        }
    }

    private PurchaseOrderPaymentInstructionDto toDto(PurchaseOrderPaymentInstructionEntity entity) {
        PurchaseOrderPaymentInstructionDto dto = new PurchaseOrderPaymentInstructionDto();
        dto.setId(entity.getId());
        dto.setScheduleId(entity.getSchedule().getId());
        dto.setInstallmentNo(entity.getSchedule().getInstallmentNo());
        dto.setAmount(entity.getAmount());
        dto.setAmountThb(entity.getAmountThb());
        dto.setCurrency(entity.getCurrency());
        dto.setExchangeRate(entity.getExchangeRate());
        dto.setPlannedPaymentDate(entity.getPlannedPaymentDate());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setPaidAmountThb(entity.getPaidAmountThb());
        dto.setConfirmedPaidDate(entity.getConfirmedPaidDate());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        dto.setCreatedBy(userMapper.toDto(entity.getCreatedBy()));
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedBy(userMapper.toDto(entity.getUpdatedBy()));
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }
}
