package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderPaymentScheduleStatus;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
import com.nutalig.dto.PurchaseOrderPaymentScheduleDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.repository.PurchaseOrderPaymentScheduleRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PurchaseOrderPaymentScheduleService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderPaymentScheduleRepository scheduleRepository;

    public PurchaseOrderPaymentScheduleService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderPaymentScheduleRepository scheduleRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderPaymentScheduleDto> getSchedules(String purchaseOrderNo)
            throws DataNotFoundException {
        if (!purchaseOrderRepository.existsById(purchaseOrderNo)) {
            throw new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found.");
        }
        return scheduleRepository
                .findByPurchaseOrderPurchaseOrderNoOrderByInstallmentNoAsc(purchaseOrderNo)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public void initializeSchedules(PurchaseOrderEntity purchaseOrder, ZonedDateTime now) {
        if (purchaseOrder == null || !purchaseOrder.getPaymentSchedules().isEmpty()) return;

        String paymentTermCode = purchaseOrder.getPaymentTerm() == null
                || purchaseOrder.getPaymentTerm().getId() == null
                ? null
                : purchaseOrder.getPaymentTerm().getId().getCode();
        List<BigDecimal> percentages = derivePercentages(paymentTermCode);
        BigDecimal expectedAllocated = BigDecimal.ZERO;
        BigDecimal expectedThbAllocated = BigDecimal.ZERO;
        BigDecimal grandTotal = defaultAmount(purchaseOrder.getGrandTotal());
        BigDecimal grandTotalThb = defaultAmount(purchaseOrder.getGrandTotalThb());

        for (int index = 0; index < percentages.size(); index++) {
            boolean last = index == percentages.size() - 1;
            BigDecimal percentage = percentages.get(index);
            BigDecimal expectedAmount = last
                    ? grandTotal.subtract(expectedAllocated)
                    : percentageAmount(grandTotal, percentage);
            BigDecimal expectedAmountThb = last
                    ? grandTotalThb.subtract(expectedThbAllocated)
                    : percentageAmount(grandTotalThb, percentage);

            PurchaseOrderPaymentScheduleEntity schedule = new PurchaseOrderPaymentScheduleEntity();
            schedule.setInstallmentNo(index + 1);
            schedule.setPaymentType(resolvePaymentType(index, percentages.size(), paymentTermCode));
            schedule.setPercentage(percentage.setScale(4, RoundingMode.HALF_UP));
            schedule.setExpectedAmount(expectedAmount.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedAmountThb(expectedAmountThb.setScale(5, RoundingMode.HALF_UP));
            schedule.setPaidAmount(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            schedule.setPaidAmountThb(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            schedule.setStatus(PurchaseOrderPaymentScheduleStatus.UNPAID);
            schedule.setCreatedDate(now);
            schedule.setUpdatedDate(now);
            purchaseOrder.addPaymentSchedule(schedule);

            expectedAllocated = expectedAllocated.add(expectedAmount);
            expectedThbAllocated = expectedThbAllocated.add(expectedAmountThb);
        }
    }

    public void recalculateSchedules(PurchaseOrderEntity purchaseOrder) {
        if (purchaseOrder == null) return;
        for (PurchaseOrderPaymentScheduleEntity schedule : purchaseOrder.getPaymentSchedules()) {
            BigDecimal paidAmount = schedule.getPayments().stream()
                    .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                    .map(PurchaseOrderPaymentEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paidAmountThb = schedule.getPayments().stream()
                    .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.APPROVED)
                    .map(PurchaseOrderPaymentEntity::getAmountThb)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            schedule.setPaidAmount(paidAmount.setScale(5, RoundingMode.HALF_UP));
            schedule.setPaidAmountThb(paidAmountThb.setScale(5, RoundingMode.HALF_UP));
            if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                schedule.setStatus(PurchaseOrderPaymentScheduleStatus.UNPAID);
            } else if (paidAmount.compareTo(defaultAmount(schedule.getExpectedAmount())) >= 0) {
                schedule.setStatus(PurchaseOrderPaymentScheduleStatus.PAID);
            } else {
                schedule.setStatus(PurchaseOrderPaymentScheduleStatus.PARTIALLY_PAID);
            }
        }
    }

    public void recalculateExpectedAmounts(PurchaseOrderEntity purchaseOrder) {
        List<PurchaseOrderPaymentScheduleEntity> schedules = purchaseOrder.getPaymentSchedules().stream()
                .sorted(java.util.Comparator.comparing(PurchaseOrderPaymentScheduleEntity::getInstallmentNo))
                .toList();
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal allocatedThb = BigDecimal.ZERO;
        BigDecimal grandTotal = defaultAmount(purchaseOrder.getGrandTotal());
        BigDecimal grandTotalThb = defaultAmount(purchaseOrder.getGrandTotalThb());
        for (int index = 0; index < schedules.size(); index++) {
            PurchaseOrderPaymentScheduleEntity schedule = schedules.get(index);
            boolean last = index == schedules.size() - 1;
            BigDecimal amount = last
                    ? grandTotal.subtract(allocated)
                    : percentageAmount(grandTotal, schedule.getPercentage());
            BigDecimal amountThb = last
                    ? grandTotalThb.subtract(allocatedThb)
                    : percentageAmount(grandTotalThb, schedule.getPercentage());
            schedule.setExpectedAmount(amount.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedAmountThb(amountThb.setScale(5, RoundingMode.HALF_UP));
            allocated = allocated.add(amount);
            allocatedThb = allocatedThb.add(amountThb);
        }
    }

    List<BigDecimal> derivePercentages(String paymentTermCode) {
        String normalized = StringUtils.trimToEmpty(paymentTermCode).toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("DEP")) return List.of(ONE_HUNDRED);

        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        List<BigDecimal> percentages = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        while (matcher.find()) {
            BigDecimal percentage = new BigDecimal(matcher.group());
            if (percentage.compareTo(BigDecimal.ZERO) <= 0) continue;
            percentages.add(percentage);
            runningTotal = runningTotal.add(percentage);
            if (runningTotal.compareTo(ONE_HUNDRED) >= 0) break;
        }
        if (percentages.isEmpty()) return List.of(ONE_HUNDRED);

        BigDecimal total = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (percentages.size() == 1 && total.compareTo(ONE_HUNDRED) < 0) {
            percentages.add(ONE_HUNDRED.subtract(total));
            return percentages;
        }
        return total.compareTo(ONE_HUNDRED) == 0 ? percentages : List.of(ONE_HUNDRED);
    }

    private PurchaseOrderPaymentType resolvePaymentType(int index, int size, String paymentTermCode) {
        boolean depositTerm = StringUtils.startsWithIgnoreCase(StringUtils.trimToEmpty(paymentTermCode), "DEP");
        if (size == 1) return depositTerm ? PurchaseOrderPaymentType.DEPOSIT : PurchaseOrderPaymentType.BALANCE;
        if (index == 0) return PurchaseOrderPaymentType.DEPOSIT;
        if (index == size - 1) return PurchaseOrderPaymentType.BALANCE;
        return PurchaseOrderPaymentType.INSTALLMENT;
    }

    private PurchaseOrderPaymentScheduleDto toDto(PurchaseOrderPaymentScheduleEntity entity) {
        BigDecimal pendingAmount = entity.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.PENDING)
                .map(PurchaseOrderPaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmountThb = entity.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.PENDING)
                .map(PurchaseOrderPaymentEntity::getAmountThb)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PurchaseOrderPaymentScheduleDto dto = new PurchaseOrderPaymentScheduleDto();
        dto.setId(entity.getId());
        dto.setInstallmentNo(entity.getInstallmentNo());
        dto.setPaymentType(entity.getPaymentType());
        dto.setPercentage(entity.getPercentage());
        dto.setExpectedAmount(entity.getExpectedAmount());
        dto.setExpectedAmountThb(entity.getExpectedAmountThb());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setPaidAmountThb(entity.getPaidAmountThb());
        dto.setPendingAmount(pendingAmount.setScale(5, RoundingMode.HALF_UP));
        dto.setPendingAmountThb(pendingAmountThb.setScale(5, RoundingMode.HALF_UP));
        dto.setOutstandingAmount(defaultAmount(entity.getExpectedAmount())
                .subtract(defaultAmount(entity.getPaidAmount())).max(BigDecimal.ZERO)
                .setScale(5, RoundingMode.HALF_UP));
        dto.setOutstandingAmountThb(defaultAmount(entity.getExpectedAmountThb())
                .subtract(defaultAmount(entity.getPaidAmountThb())).max(BigDecimal.ZERO)
                .setScale(5, RoundingMode.HALF_UP));
        dto.setStatus(entity.getStatus());
        dto.setDueDate(entity.getDueDate());
        return dto;
    }

    private BigDecimal percentageAmount(BigDecimal total, BigDecimal percentage) {
        return total.multiply(percentage).divide(ONE_HUNDRED, 5, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
