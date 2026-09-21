package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderPaymentType;
import com.nutalig.constant.SystemConstant;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.entity.id.SystemConfigId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseOrderPaymentScheduleServiceTest {

    private PurchaseOrderPaymentScheduleService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderPaymentScheduleService(null, null);
    }

    @Test
    void dep50CreatesDepositAndBalanceSchedules() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("DEP50", "1000.00", "35000.00");

        service.initializeSchedules(purchaseOrder, ZonedDateTime.now());
        service.initializeSchedules(purchaseOrder, ZonedDateTime.now());

        List<PurchaseOrderPaymentScheduleEntity> schedules = sortedSchedules(purchaseOrder);
        assertEquals(2, schedules.size());
        assertEquals(PurchaseOrderPaymentType.DEPOSIT, schedules.get(0).getPaymentType());
        assertEquals(new BigDecimal("50.0000"), schedules.get(0).getPercentage());
        assertEquals(new BigDecimal("500.00000"), schedules.get(0).getExpectedAmount());
        assertEquals(PurchaseOrderPaymentType.BALANCE, schedules.get(1).getPaymentType());
        assertEquals(new BigDecimal("500.00000"), schedules.get(1).getExpectedAmount());
    }

    @Test
    void dep305020CreatesThreeSchedulesAndPreservesTotal() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("DEP_30_50_20", "999.99", "999.99");

        service.initializeSchedules(purchaseOrder, ZonedDateTime.now());

        List<PurchaseOrderPaymentScheduleEntity> schedules = sortedSchedules(purchaseOrder);
        assertEquals(3, schedules.size());
        assertEquals(PurchaseOrderPaymentType.DEPOSIT, schedules.get(0).getPaymentType());
        assertEquals(PurchaseOrderPaymentType.INSTALLMENT, schedules.get(1).getPaymentType());
        assertEquals(PurchaseOrderPaymentType.BALANCE, schedules.get(2).getPaymentType());
        assertEquals(
                new BigDecimal("999.99000"),
                schedules.stream()
                        .map(PurchaseOrderPaymentScheduleEntity::getExpectedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    @Test
    void nonDepositTermCreatesSingleFullBalanceSchedule() {
        PurchaseOrderEntity purchaseOrder = purchaseOrder("CR30", "125.00", "125.00");

        service.initializeSchedules(purchaseOrder, ZonedDateTime.now());

        PurchaseOrderPaymentScheduleEntity schedule = sortedSchedules(purchaseOrder).get(0);
        assertEquals(PurchaseOrderPaymentType.BALANCE, schedule.getPaymentType());
        assertEquals(new BigDecimal("100.0000"), schedule.getPercentage());
        assertEquals(new BigDecimal("125.00000"), schedule.getExpectedAmount());
    }

    @Test
    void ignoresNumbersAfterPercentagesAlreadyReachOneHundred() {
        assertEquals(
                List.of(new BigDecimal("30"), new BigDecimal("70")),
                service.derivePercentages("DEP_30_70_30_DAYS")
        );
    }

    private PurchaseOrderEntity purchaseOrder(String paymentTermCode, String total, String totalThb) {
        SystemConfigId configId = new SystemConfigId();
        configId.setGroupCode(SystemConstant.SUPPLIER_PAYMENT_TERM);
        configId.setCode(paymentTermCode);
        SystemConfigEntity paymentTerm = new SystemConfigEntity();
        paymentTerm.setId(configId);

        PurchaseOrderEntity purchaseOrder = new PurchaseOrderEntity();
        purchaseOrder.setPurchaseOrderNo("NTL-PO-TEST");
        purchaseOrder.setPaymentTerm(paymentTerm);
        purchaseOrder.setGrandTotal(new BigDecimal(total));
        purchaseOrder.setGrandTotalThb(new BigDecimal(totalThb));
        return purchaseOrder;
    }

    private List<PurchaseOrderPaymentScheduleEntity> sortedSchedules(PurchaseOrderEntity purchaseOrder) {
        return purchaseOrder.getPaymentSchedules().stream()
                .sorted(Comparator.comparing(PurchaseOrderPaymentScheduleEntity::getInstallmentNo))
                .toList();
    }
}
