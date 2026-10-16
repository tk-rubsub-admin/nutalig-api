package com.nutalig.service;

import com.nutalig.constant.Currency;
import com.nutalig.constant.InvoiceStatus;
import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.entity.CustomerAnnualSalesEntity;
import com.nutalig.entity.CustomerAnnualSalesOrderEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.id.CustomerAnnualSalesId;
import com.nutalig.repository.CustomerAnnualSalesOrderRepository;
import com.nutalig.repository.CustomerAnnualSalesRepository;
import com.nutalig.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerAnnualSalesService {

    private static final Set<InvoiceStatus> EXCLUDED_INVOICE_STATUSES = Set.of(
            InvoiceStatus.CANCELLED, InvoiceStatus.VOID
    );
    private static final Set<SalesOrderStatus> EXCLUDED_SALES_ORDER_STATUSES = Set.of(
            SalesOrderStatus.REJECTED, SalesOrderStatus.CANCELLED
    );

    private final CustomerAnnualSalesRepository annualSalesRepository;
    private final CustomerAnnualSalesOrderRepository annualSalesOrderRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void refreshForSalesOrder(SalesOrderEntity order) {
        if (order == null || order.getSalesOrderNo() == null) return;

        CustomerAnnualSalesOrderEntity previous = annualSalesOrderRepository
                .findById(order.getSalesOrderNo()).orElse(null);
        Set<CustomerAnnualSalesId> affectedBuckets = new HashSet<>();
        if (previous != null) affectedBuckets.add(toSummaryId(previous));

        String customerId = order.getCustomer() == null ? null : order.getCustomer().getId();
        Currency currency = order.getCurrency();
        BigDecimal grandTotal = order.getGrandTotal() == null ? BigDecimal.ZERO : order.getGrandTotal();
        BigDecimal approvedPaymentTotal = invoiceRepository.sumApprovedPaymentAmountForSalesOrder(
                order.getSalesOrderNo(), EXCLUDED_INVOICE_STATUSES);
        ZonedDateTime latestApprovedPaymentDate = invoiceRepository.findLatestApprovedPaymentDateForSalesOrder(
                order.getSalesOrderNo(), EXCLUDED_INVOICE_STATUSES);

        CustomerAnnualSalesOrderEntity current = null;
        if (customerId != null && currency != null && grandTotal.signum() > 0
                && approvedPaymentTotal != null && approvedPaymentTotal.compareTo(grandTotal) >= 0
                && latestApprovedPaymentDate != null
                && !EXCLUDED_SALES_ORDER_STATUSES.contains(order.getStatus())) {
            LocalDate paidInFullDate = latestApprovedPaymentDate
                    .withZoneSameInstant(ZoneId.of("Asia/Bangkok")).toLocalDate();
            current = new CustomerAnnualSalesOrderEntity();
            current.setSalesOrderNo(order.getSalesOrderNo());
            current.setCustomerId(customerId);
            current.setSalesYear(paidInFullDate.getYear());
            current.setCurrency(currency);
            current.setPaidInFullDate(paidInFullDate);
            current.setTotalOrderAmount(grandTotal);
            annualSalesOrderRepository.save(current);
            affectedBuckets.add(toSummaryId(current));
        } else {
            annualSalesOrderRepository.deleteById(order.getSalesOrderNo());
        }

        affectedBuckets.forEach(this::refreshBucket);
    }

    private void refreshBucket(CustomerAnnualSalesId id) {
        long orderCount = annualSalesOrderRepository.countAnnualOrders(
                id.getCustomerId(), id.getSalesYear(), id.getCurrency());
        if (orderCount == 0) {
            annualSalesRepository.deleteById(id);
            return;
        }
        CustomerAnnualSalesEntity summary = annualSalesRepository.findById(id)
                .orElseGet(CustomerAnnualSalesEntity::new);
        summary.setId(id);
        summary.setTotalOrderAmount(annualSalesOrderRepository.sumAnnualAmount(
                id.getCustomerId(), id.getSalesYear(), id.getCurrency()));
        summary.setOrderCount(Math.toIntExact(orderCount));
        annualSalesRepository.save(summary);
    }

    private CustomerAnnualSalesId toSummaryId(CustomerAnnualSalesOrderEntity order) {
        return new CustomerAnnualSalesId(order.getCustomerId(), order.getSalesYear(), order.getCurrency());
    }
}
