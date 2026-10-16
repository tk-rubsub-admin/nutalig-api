package com.nutalig.service;

import com.nutalig.constant.SalesPerformanceType;
import com.nutalig.entity.InvoiceEntity;
import com.nutalig.entity.InvoicePaymentEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.SalesPerformanceTransactionEntity;
import com.nutalig.dto.SalesMonthlyPerformanceDto;
import com.nutalig.repository.SalesPerformanceTransactionRepository;
import com.nutalig.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesPerformanceService {

    private final SalesPerformanceTransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void refreshForSalesOrder(SalesOrderEntity order) {
        if (order == null || order.getSalesOrderNo() == null) return;
        transactionRepository.deleteBySalesOrderNo(order.getSalesOrderNo());
        invoiceRepository.findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(order.getSalesOrderNo())
                .stream()
                .filter(invoice -> invoice.getStatus() != com.nutalig.constant.InvoiceStatus.CANCELLED
                        && invoice.getStatus() != com.nutalig.constant.InvoiceStatus.VOID)
                .forEach(invoice -> invoice.getPayments().stream()
                        .filter(payment -> payment.getStatus() == com.nutalig.constant.InvoicePaymentStatus.APPROVE)
                        .forEach(payment -> recordApprovedPayment(invoice, payment)));
    }

    @Transactional
    public void recordApprovedPayment(InvoiceEntity invoice, InvoicePaymentEntity payment) {
        if (invoice == null || payment == null || payment.getId() == null
                || payment.getStatus() == null || !"APPROVE".equals(payment.getStatus().name())) return;

        SalesOrderEntity order = invoice.getSalesOrder();
        if (order == null || order.getSales() == null || payment.getPaymentDate() == null
                || invoice.getInstallmentNo() == null || invoice.getInstallmentCount() == null) return;

        SalesPerformanceTransactionEntity event = transactionRepository.findById(payment.getId())
                .orElseGet(SalesPerformanceTransactionEntity::new);
        event.setInvoicePaymentId(payment.getId());
        event.setSalesOrderNo(order.getSalesOrderNo());
        event.setInvoiceNo(invoice.getInvoiceNo());
        event.setSalesId(order.getSales().getEmployeeId());
        event.setPerformanceType(resolveType(invoice));
        event.setPaymentDate(payment.getPaymentDate().withZoneSameInstant(ZoneId.of("Asia/Bangkok")).toLocalDate());
        event.setCurrency(invoice.getCurrency());
        event.setAmount(payment.getAmount());
        transactionRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<SalesMonthlyPerformanceDto> getMonthlySummary(LocalDate dateFrom, LocalDate dateTo, String salesId) {
        return transactionRepository.findMonthlySummary(dateFrom, dateTo, salesId).stream().map(row -> {
            SalesMonthlyPerformanceDto dto = new SalesMonthlyPerformanceDto();
            dto.setSalesId(row.getSalesId());
            dto.setYear(row.getSalesYear());
            dto.setMonth(row.getSalesMonth());
            dto.setCurrency(com.nutalig.constant.Currency.valueOf(row.getCurrency()));
            dto.setDepositAmount(row.getDepositAmount());
            dto.setSalesAmount(row.getSalesAmount());
            dto.setInterimInstallmentAmount(row.getInterimInstallmentAmount());
            return dto;
        }).toList();
    }

    private SalesPerformanceType resolveType(InvoiceEntity invoice) {
        if (invoice.getInstallmentCount() == 1) return SalesPerformanceType.SALE;
        if (invoice.getInstallmentNo() == 1) return SalesPerformanceType.DEPOSIT;
        if (invoice.getInstallmentNo().equals(invoice.getInstallmentCount())) return SalesPerformanceType.SALE;
        return SalesPerformanceType.INTERIM_INSTALLMENT;
    }
}
