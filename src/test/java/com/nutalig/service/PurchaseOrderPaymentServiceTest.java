package com.nutalig.service;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderPaymentLifecycleStatus;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseOrderPaymentServiceTest {

    private PurchaseOrderPaymentService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderPaymentService(
                null,
                null,
                null,
                null,
                null,
                null,
                new PurchaseOrderPaymentScheduleService(null, null)
        );
    }

    @Test
    void initializePaymentSummarySetsOutstandingFromGrandTotal() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("100.00", "100.00");

        service.initializePaymentSummary(purchaseOrder);

        assertEquals(PurchaseOrderPaymentLifecycleStatus.UNPAID, purchaseOrder.getPaymentStatus());
        assertEquals(new BigDecimal("0.00000"), purchaseOrder.getPaidTotal());
        assertEquals(new BigDecimal("100.00000"), purchaseOrder.getOutstandingTotal());
    }

    @Test
    void recalculatePaymentSummaryCountsOnlyApprovedPayments() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("100.00", "100.00");
        purchaseOrder.addPayment(payment("30.00", PurchaseOrderPaymentStatus.APPROVED));
        purchaseOrder.addPayment(payment("20.00", PurchaseOrderPaymentStatus.PENDING));

        service.recalculatePaymentSummary(purchaseOrder);

        assertEquals(PurchaseOrderPaymentLifecycleStatus.PARTIALLY_PAID, purchaseOrder.getPaymentStatus());
        assertEquals(new BigDecimal("30.00000"), purchaseOrder.getPaidTotal());
        assertEquals(new BigDecimal("70.00000"), purchaseOrder.getOutstandingTotal());
        assertEquals(PurchaseOrderStatus.AWAITING_PAYMENT, purchaseOrder.getStatus());
    }

    @Test
    void recalculatePaymentSummaryMarksPurchaseOrderPaidWhenFullyPaid() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("100.00", "100.00");
        purchaseOrder.addPayment(payment("100.00", PurchaseOrderPaymentStatus.APPROVED));

        service.recalculatePaymentSummary(purchaseOrder);

        assertEquals(PurchaseOrderPaymentLifecycleStatus.PAID, purchaseOrder.getPaymentStatus());
        assertEquals(BigDecimal.ZERO.setScale(5), purchaseOrder.getOutstandingTotal());
        assertEquals(PurchaseOrderStatus.PAID, purchaseOrder.getStatus());
    }

    @Test
    void validateTotalChangeRejectsTotalBelowPendingAndApprovedPayments() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("40.00", "40.00");
        purchaseOrder.addPayment(payment("30.00", PurchaseOrderPaymentStatus.APPROVED));
        purchaseOrder.addPayment(payment("20.00", PurchaseOrderPaymentStatus.PENDING));

        assertThrows(
                InvalidRequestException.class,
                () -> service.validateAndRecalculateAfterOrderTotalChange(purchaseOrder)
        );
    }

    @Test
    void resolvePaymentExchangeRateUsesPurchaseOrderTotalsWhenRateIsNotProvided() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("100.00", "3500.00");
        purchaseOrder.setCurrency(Currency.CNY);

        BigDecimal exchangeRate = service.resolvePaymentExchangeRate(purchaseOrder, null);

        assertEquals(new BigDecimal("35.000000"), exchangeRate);
    }

    private PurchaseOrderEntity purchaseOrder(String grandTotal, String grandTotalThb) {
        PurchaseOrderEntity purchaseOrder = new PurchaseOrderEntity();
        purchaseOrder.setPurchaseOrderNo("NTL-PO-TEST");
        purchaseOrder.setGrandTotal(new BigDecimal(grandTotal));
        purchaseOrder.setGrandTotalThb(new BigDecimal(grandTotalThb));
        purchaseOrder.setStatus(PurchaseOrderStatus.AWAITING_PAYMENT);
        return purchaseOrder;
    }

    private PurchaseOrderPaymentEntity payment(String amount, PurchaseOrderPaymentStatus status) {
        PurchaseOrderPaymentEntity payment = new PurchaseOrderPaymentEntity();
        payment.setAmount(new BigDecimal(amount));
        payment.setAmountThb(new BigDecimal(amount));
        payment.setStatus(status);
        return payment;
    }
}
