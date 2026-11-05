package com.nutalig.service;

import com.nutalig.constant.InvoiceStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
import com.nutalig.constant.SalesOrderPaymentScheduleStatus;
import com.nutalig.entity.InvoiceEntity;
import com.nutalig.entity.InvoicePaymentEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.SalesOrderPaymentScheduleEntity;
import com.nutalig.dto.SalesOrderPaymentScheduleDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.SalesOrderPaymentScheduleRepository;
import com.nutalig.repository.SalesOrderRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SalesOrderPaymentScheduleService {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderPaymentScheduleRepository scheduleRepository;
    private final InvoiceRepository invoiceRepository;

    public SalesOrderPaymentScheduleService(
            SalesOrderRepository salesOrderRepository,
            SalesOrderPaymentScheduleRepository scheduleRepository,
            InvoiceRepository invoiceRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.scheduleRepository = scheduleRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesOrderPaymentScheduleDto> getSchedules(String salesOrderNo)
            throws DataNotFoundException {
        if (!salesOrderRepository.existsById(salesOrderNo)) {
            throw new DataNotFoundException("Sales order " + salesOrderNo + " not found.");
        }
        return scheduleRepository.findBySalesOrderSalesOrderNoOrderByInstallmentNoAsc(salesOrderNo)
                .stream().map(this::toDto).toList();
    }

    public void initializeSchedules(SalesOrderEntity salesOrder, ZonedDateTime now) {
        if (salesOrder == null || !salesOrder.getPaymentSchedules().isEmpty()) return;
        String term = salesOrder.getPaymentTerm() != null && salesOrder.getPaymentTerm().getId() != null
                ? salesOrder.getPaymentTerm().getId().getCode()
                : salesOrder.getCustomer() != null && salesOrder.getCustomer().getCustomerPaymentTerm() != null
                && salesOrder.getCustomer().getCustomerPaymentTerm().getId() != null
                ? salesOrder.getCustomer().getCustomerPaymentTerm().getId().getCode() : null;
        List<BigDecimal> percentages = derivePercentages(term);
        BigDecimal allocatedSubTotal = BigDecimal.ZERO;
        BigDecimal allocatedVat = BigDecimal.ZERO;
        BigDecimal total = defaultAmount(salesOrder.getGrandTotal());
        BigDecimal totalVat = defaultAmount(salesOrder.getVat());
        BigDecimal totalSubTotal = total.subtract(totalVat).max(BigDecimal.ZERO);
        for (int index = 0; index < percentages.size(); index++) {
            boolean last = index == percentages.size() - 1;
            BigDecimal percentage = percentages.get(index);
            BigDecimal expectedSubTotal = last
                    ? totalSubTotal.subtract(allocatedSubTotal)
                    : percentageAmount(totalSubTotal, percentage);
            BigDecimal expectedVat = last
                    ? totalVat.subtract(allocatedVat)
                    : percentageAmount(totalVat, percentage);
            BigDecimal expected = expectedSubTotal.add(expectedVat).setScale(5, RoundingMode.HALF_UP);
            SalesOrderPaymentScheduleEntity schedule = new SalesOrderPaymentScheduleEntity();
            schedule.setInstallmentNo(index + 1);
            schedule.setPaymentType(resolvePaymentType(index, percentages.size(), term));
            schedule.setPercentage(percentage.setScale(4, RoundingMode.HALF_UP));
            schedule.setExpectedSubTotal(expectedSubTotal.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedVat(expectedVat.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedAmount(expected.setScale(5, RoundingMode.HALF_UP));
            schedule.setPaidAmount(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            schedule.setOutstandingAmount(expected.setScale(5, RoundingMode.HALF_UP));
            schedule.setStatus(SalesOrderPaymentScheduleStatus.UNINVOICED);
            schedule.setDueDate(salesOrder.getPaymentScheduleDate());
            schedule.setCreatedDate(now);
            schedule.setUpdatedDate(now);
            salesOrder.addPaymentSchedule(schedule);
            allocatedSubTotal = allocatedSubTotal.add(expectedSubTotal);
            allocatedVat = allocatedVat.add(expectedVat);
        }
    }

    @Transactional
    public void replaceSchedulesForPaymentTerm(SalesOrderEntity salesOrder) throws InvalidRequestException {
        if (salesOrder == null) return;
        List<InvoiceEntity> invoices = invoiceRepository
                .findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(salesOrder.getSalesOrderNo())
                .stream().filter(this::isActiveInvoice).toList();
        if (!invoices.isEmpty() || salesOrder.getPaymentSchedules().stream()
                .anyMatch(schedule -> defaultAmount(schedule.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0)) {
            throw new InvalidRequestException("Sales order payment term cannot be changed after payment or invoice creation.");
        }
        // Move the old installment numbers out of the unique-key range first.
        // Hibernate may otherwise flush the replacement INSERT before the old
        // orphan rows are deleted, even when deleteAll(...).flush() is called.
        List<SalesOrderPaymentScheduleEntity> oldSchedules = new ArrayList<>(salesOrder.getPaymentSchedules());
        int temporaryInstallmentNo = -1;
        for (SalesOrderPaymentScheduleEntity schedule : oldSchedules) {
            schedule.setInstallmentNo(temporaryInstallmentNo--);
        }
        scheduleRepository.flush();
        scheduleRepository.deleteAll(oldSchedules);
        scheduleRepository.flush();
        salesOrder.getPaymentSchedules().clear();
        initializeSchedules(salesOrder, ZonedDateTime.now());
    }

    public void recalculateExpectedAmounts(SalesOrderEntity salesOrder) throws InvalidRequestException {
        if (salesOrder == null || salesOrder.getPaymentSchedules().isEmpty()) return;
        List<InvoiceEntity> invoices = invoiceRepository.findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(salesOrder.getSalesOrderNo())
                .stream().filter(this::isActiveInvoice).toList();
        if (!invoices.isEmpty()) {
            throw new InvalidRequestException("Sales order payment schedule cannot be changed after an invoice is created.");
        }
        List<SalesOrderPaymentScheduleEntity> schedules = salesOrder.getPaymentSchedules().stream()
                .sorted(Comparator.comparing(SalesOrderPaymentScheduleEntity::getInstallmentNo)).toList();
        BigDecimal total = defaultAmount(salesOrder.getGrandTotal());
        BigDecimal totalVat = defaultAmount(salesOrder.getVat());
        BigDecimal totalSubTotal = total.subtract(totalVat).max(BigDecimal.ZERO);
        BigDecimal allocatedSubTotal = BigDecimal.ZERO;
        BigDecimal allocatedVat = BigDecimal.ZERO;
        for (int index = 0; index < schedules.size(); index++) {
            SalesOrderPaymentScheduleEntity schedule = schedules.get(index);
            boolean last = index == schedules.size() - 1;
            BigDecimal expectedSubTotal = last
                    ? totalSubTotal.subtract(allocatedSubTotal)
                    : percentageAmount(totalSubTotal, schedule.getPercentage());
            BigDecimal expectedVat = last
                    ? totalVat.subtract(allocatedVat)
                    : percentageAmount(totalVat, schedule.getPercentage());
            BigDecimal expected = expectedSubTotal.add(expectedVat).setScale(5, RoundingMode.HALF_UP);
            schedule.setExpectedSubTotal(expectedSubTotal.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedVat(expectedVat.setScale(5, RoundingMode.HALF_UP));
            schedule.setExpectedAmount(expected.setScale(5, RoundingMode.HALF_UP));
            schedule.setPaidAmount(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            schedule.setOutstandingAmount(expected.setScale(5, RoundingMode.HALF_UP));
            schedule.setStatus(SalesOrderPaymentScheduleStatus.UNINVOICED);
            allocatedSubTotal = allocatedSubTotal.add(expectedSubTotal);
            allocatedVat = allocatedVat.add(expectedVat);
        }
    }

    @Transactional
    public void recalculateStatuses(String salesOrderNo) {
        scheduleRepository.findBySalesOrderSalesOrderNoOrderByInstallmentNoAsc(salesOrderNo)
                .forEach(this::recalculateStatus);
    }

    private void recalculateStatus(SalesOrderPaymentScheduleEntity schedule) {
        List<InvoiceEntity> invoices = invoiceRepository.findByPaymentScheduleId(schedule.getId())
                .stream().filter(this::isActiveInvoice).toList();
        if (invoices.isEmpty()) {
            schedule.setPaidAmount(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            schedule.setOutstandingAmount(defaultAmount(schedule.getExpectedAmount()).setScale(5, RoundingMode.HALF_UP));
            schedule.setStatus(SalesOrderPaymentScheduleStatus.UNINVOICED);
            return;
        }
        BigDecimal paid = invoices.stream().flatMap(i -> i.getPayments().stream())
                .filter(p -> p.getStatus() == com.nutalig.constant.InvoicePaymentStatus.APPROVE)
                .map(InvoicePaymentEntity::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = defaultAmount(schedule.getExpectedAmount());
        schedule.setPaidAmount(paid.setScale(5, RoundingMode.HALF_UP));
        schedule.setOutstandingAmount(expected.subtract(paid).max(BigDecimal.ZERO).setScale(5, RoundingMode.HALF_UP));
        schedule.setStatus(paid.signum() <= 0 ? SalesOrderPaymentScheduleStatus.UNPAID
                : paid.compareTo(expected) >= 0 ? SalesOrderPaymentScheduleStatus.PAID
                : SalesOrderPaymentScheduleStatus.PARTIALLY_PAID);
    }

    public SalesOrderPaymentScheduleEntity resolveForInvoice(Long id, String salesOrderNo)
            throws DataNotFoundException, InvalidRequestException {
        SalesOrderPaymentScheduleEntity schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Payment schedule " + id + " not found."));
        if (schedule.getSalesOrder() == null || !Objects.equals(schedule.getSalesOrder().getSalesOrderNo(), salesOrderNo)) {
            throw new InvalidRequestException("Payment schedule does not belong to sales order");
        }
        if (invoiceRepository.findByPaymentScheduleId(id).stream().anyMatch(this::isActiveInvoice)) {
            throw new InvalidRequestException("Payment schedule already has an invoice");
        }
        return schedule;
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

    private PurchaseOrderPaymentType resolvePaymentType(int index, int size, String term) {
        if (size == 1) return StringUtils.startsWithIgnoreCase(StringUtils.trimToEmpty(term), "DEP")
                ? PurchaseOrderPaymentType.DEPOSIT : PurchaseOrderPaymentType.BALANCE;
        return index == 0 ? PurchaseOrderPaymentType.DEPOSIT
                : index == size - 1 ? PurchaseOrderPaymentType.BALANCE : PurchaseOrderPaymentType.INSTALLMENT;
    }

    private SalesOrderPaymentScheduleDto toDto(SalesOrderPaymentScheduleEntity entity) {
        SalesOrderPaymentScheduleDto dto = new SalesOrderPaymentScheduleDto();
        dto.setId(entity.getId());
        dto.setSalesOrderNo(entity.getSalesOrder().getSalesOrderNo());
        dto.setInstallmentNo(entity.getInstallmentNo());
        dto.setInstallmentCount(entity.getSalesOrder().getPaymentSchedules().size());
        dto.setPaymentType(entity.getPaymentType());
        dto.setPercentage(entity.getPercentage());
        dto.setExpectedSubTotal(entity.getExpectedSubTotal());
        dto.setExpectedVat(entity.getExpectedVat());
        dto.setExpectedAmount(entity.getExpectedAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setOutstandingAmount(entity.getOutstandingAmount());
        dto.setStatus(entity.getStatus());
        dto.setDueDate(entity.getDueDate());
        invoiceRepository.findByPaymentScheduleId(entity.getId()).stream().filter(this::isActiveInvoice)
                .findFirst().ifPresent(i -> dto.setInvoiceNo(i.getInvoiceNo()));
        return dto;
    }

    private boolean isActiveInvoice(InvoiceEntity invoice) {
        return invoice != null && !List.of(InvoiceStatus.DRAFT, InvoiceStatus.CANCELLED, InvoiceStatus.VOID).contains(invoice.getStatus());
    }

    private BigDecimal percentageAmount(BigDecimal total, BigDecimal percentage) {
        return total.multiply(percentage).divide(ONE_HUNDRED, 5, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
