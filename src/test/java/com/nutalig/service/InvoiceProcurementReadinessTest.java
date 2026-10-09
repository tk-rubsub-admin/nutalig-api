package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.SystemConfigId;
import com.nutalig.mapper.*;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.SalesOrderRepository;
import com.nutalig.security.JwtUtil;
import com.nutalig.utils.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceProcurementReadinessTest {
    @Mock InvoiceRepository invoiceRepository;
    @Mock SalesOrderRepository salesOrderRepository;
    @Mock SalesOrderService salesOrderService;
    @Mock ActivityHistoryService activityHistoryService;
    @Mock CustomerMapper customerMapper;
    @Mock EmployeeMapper employeeMapper;
    @Mock UserMapper userMapper;
    @Mock SystemConfigMapper systemConfigMapper;
    @InjectMocks InvoiceService service;

    private SalesOrderEntity so;
    private final List<InvoiceEntity> invoices = new ArrayList<>();

    @BeforeEach
    void setup() {
        invoices.clear();
        so = new SalesOrderEntity();
        so.setSalesOrderNo("NTL-SO2026090029");
        so.setStatus(SalesOrderStatus.CREATED);
        so.setProcurementStatus(ProcurementStatus.NOT_READY);
        lenient().when(invoiceRepository.findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(so.getSalesOrderNo()))
                .thenAnswer(invocation -> invoices);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private InvoiceEntity invoice(String number, String term, String amount) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNo(number);
        invoice.setSalesOrder(so);
        invoice.setStatus(InvoiceStatus.AWAITING_VALIDATION);
        invoice.setCreatedDate(ZonedDateTime.of(2026, 10, 9, 10, 0, 0, 0, DateUtil.getTimeZone()));
        invoice.setSubTotal(new BigDecimal("20700.00"));
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setAmount(new BigDecimal(amount));
        invoice.setGrandTotal(new BigDecimal(amount));
        invoice.setPaidTotal(new BigDecimal(amount));
        invoice.setOutstandingTotal(BigDecimal.ZERO);
        if (term != null) {
            SystemConfigEntity config = new SystemConfigEntity();
            SystemConfigId id = new SystemConfigId();
            id.setGroupCode(SystemConstant.CUSTOMER_PAYMENT_TERM);
            id.setCode(term);
            config.setId(id);
            invoice.setCustomerPaymentTerm(config);
        }
        invoices.add(invoice);
        return invoice;
    }

    private InvoicePaymentEntity payment(InvoiceEntity invoice, long id, String amount) {
        InvoicePaymentEntity payment = new InvoicePaymentEntity();
        payment.setId(id);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(InvoicePaymentStatus.PENDING);
        invoice.addPayment(payment);
        return payment;
    }

    private void approve(InvoiceEntity invoice, long paymentId) throws Exception {
        when(invoiceRepository.findById(invoice.getInvoiceNo())).thenReturn(Optional.of(invoice));
        try (var jwt = mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.isValid("test-token")).thenReturn(true);
            jwt.when(() -> JwtUtil.getClaim("test-token", "action")).thenReturn("awaiting-validation-view");
            jwt.when(() -> JwtUtil.getSubject("test-token")).thenReturn(invoice.getInvoiceNo() + "|" + paymentId);
            service.approveAwaitingValidationByToken("test-token");
        }
    }

    @ParameterizedTest
    @CsvSource({"DEP50,10350.00", "DEP30_BBS,6210.00", "DEP35_35_30_BBS,7245.00",
            "AFS100,20700.00", "OTHER,20700.00"})
    void approvingTheFullyPaidFirstInstallmentMakesTheOrderReady(String term, String amount) throws Exception {
        var invoice = invoice("NTL-INV2026100004", term, amount);
        var payment = payment(invoice, 47L, amount);
        approve(invoice, 47L);
        assertEquals(InvoicePaymentStatus.APPROVE, payment.getStatus());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(ProcurementStatus.READY_FOR_PO, so.getProcurementStatus());
        verify(salesOrderRepository).save(so);
        verify(activityHistoryService).record(eq(ActivityEntityType.SALES_ORDER), eq(so.getSalesOrderNo()),
                eq("PUBLIC_TOKEN"), any(), eq(ActivityAction.STATUS_CHANGE), any(), anyString(),
                argThat(detail -> detail instanceof Map<?, ?> payload
                        && "FIRST_INSTALLMENT_INVOICE_PAID".equals(payload.get("trigger"))));
    }

    @Test
    void requiresApprovedPaymentOfTheVatInclusiveInvoiceTotal() throws Exception {
        var invoice = invoice("INV-1", "DEP30_BBS", "6210.00");
        invoice.setGrandTotal(new BigDecimal("6644.70"));
        invoice.setPaidTotal(invoice.getGrandTotal());
        invoice.setVat(new BigDecimal("434.70"));
        payment(invoice, 47L, "6210.00");
        payment(invoice, 48L, "434.70");
        approve(invoice, 47L);
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(ProcurementStatus.NOT_READY, so.getProcurementStatus());
        approve(invoice, 48L);
        assertEquals(ProcurementStatus.READY_FOR_PO, so.getProcurementStatus());
        verify(salesOrderRepository).save(so);
    }

    @Test
    void partialPaymentDoesNotMakeTheOrderReady() throws Exception {
        var invoice = invoice("INV-1", "DEP30_BBS", "6210.00");
        invoice.setPaidTotal(new BigDecimal("3000.00"));
        invoice.setOutstandingTotal(new BigDecimal("3210.00"));
        payment(invoice, 47L, "3000.00");
        approve(invoice, 47L);
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
        assertEquals(ProcurementStatus.NOT_READY, so.getProcurementStatus());
        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void approvalOfTheSecond35PercentInvoiceDoesNotReplaceAnUnpaidFirstInstallment() throws Exception {
        var first = invoice("INV-1", "DEP35_35_30_BBS", "7245.00");
        first.setStatus(InvoiceStatus.ISSUED);
        var second = invoice("INV-2", "DEP35_35_30_BBS", "7245.00");
        second.setCreatedDate(first.getCreatedDate().plusDays(1));
        payment(second, 47L, "7245.00");
        approve(second, 47L);
        assertEquals(ProcurementStatus.NOT_READY, so.getProcurementStatus());
        verify(salesOrderRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"DRAFT", "CANCELLED", "VOID"})
    void ignoresDocumentsThatAreNotIssuedInstallments(InvoiceStatus ignoredStatus) throws Exception {
        var ignored = invoice("INV-0", "DEP30_BBS", "6210.00");
        ignored.setStatus(ignoredStatus);
        ignored.setCreatedDate(ignored.getCreatedDate().minusDays(1));
        var first = invoice("INV-1", "DEP30_BBS", "6210.00");
        payment(first, 47L, "6210.00");
        approve(first, 47L);
        assertEquals(ProcurementStatus.READY_FOR_PO, so.getProcurementStatus());
    }

    @Test
    void aDifferentInvoiceAmountDoesNotMatchTheFirstInstallmentTerm() throws Exception {
        var invoice = invoice("INV-1", "DEP30_BBS", "10350.00");
        payment(invoice, 47L, "10350.00");
        approve(invoice, 47L);
        assertEquals(ProcurementStatus.NOT_READY, so.getProcurementStatus());
        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void usesTheAmountAfterDiscountAndRoundsToTwoDecimals() throws Exception {
        var invoice = invoice("INV-1", "DEP50", "500.01");
        invoice.setSubTotal(new BigDecimal("1100.01"));
        invoice.setDiscount(new BigDecimal("100.00"));
        payment(invoice, 47L, "500.01");
        approve(invoice, 47L);
        assertEquals(ProcurementStatus.READY_FOR_PO, so.getProcurementStatus());
    }

    @ParameterizedTest
    @EnumSource(value = ProcurementStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "NOT_READY")
    void preservesAnExistingReadyOverrideOrPoCreatedStatus(ProcurementStatus existingStatus) throws Exception {
        so.setProcurementStatus(existingStatus);
        var invoice = invoice("INV-1", "DEP30_BBS", "6210.00");
        payment(invoice, 47L, "6210.00");
        approve(invoice, 47L);
        assertEquals(existingStatus, so.getProcurementStatus());
        verify(salesOrderRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = SalesOrderStatus.class, names = {"CANCELLED", "REJECTED"})
    void doesNotMakeACancelledOrRejectedOrderReady(SalesOrderStatus status) throws Exception {
        so.setStatus(status);
        var invoice = invoice("INV-1", "DEP30_BBS", "6210.00");
        payment(invoice, 47L, "6210.00");
        approve(invoice, 47L);
        assertEquals(ProcurementStatus.NOT_READY, so.getProcurementStatus());
        verify(salesOrderRepository, never()).save(any());
    }
}
