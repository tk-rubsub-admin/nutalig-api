package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderPaymentScheduleStatus;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import com.nutalig.dto.PurchaseOrderDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.repository.RequestPriceDetailRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.utils.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderStartRunServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private UserRepository userRepository;
    @Mock private SupplierMapper supplierMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private UserMapper userMapper;
    @Mock private RequestPriceDetailRepository requestPriceDetailRepository;
    @Mock private PurchaseOrderMilestoneService purchaseOrderMilestoneService;
    @Mock private ActivityHistoryService activityHistoryService;
    @Mock private SystemConfigService systemConfigService;
    @InjectMocks private PurchaseOrderService service;
    @Captor private ArgumentCaptor<Map<String, Object>> historyDetail;

    private PurchaseOrderEntity purchaseOrder;

    @BeforeEach
    void setUp() {
        purchaseOrder = new PurchaseOrderEntity();
        purchaseOrder.setPurchaseOrderNo("NTL-PO-TEST");
        purchaseOrder.setStatus(PurchaseOrderStatus.AWAITING_PAYMENT);
        purchaseOrder.setDocDate(LocalDate.now(DateUtil.getTimeZone()));
        purchaseOrder.setProductionLeadTimeDay(7);
        when(purchaseOrderRepository.findByIdForUpdate("NTL-PO-TEST"))
                .thenReturn(Optional.of(purchaseOrder));
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"CREATED", "AWAITING_PAYMENT", "PAID"})
    void startsWithoutPaymentScheduleOrApprovedPaymentWithOverrideReason(PurchaseOrderStatus status) throws Exception {
        purchaseOrder.setStatus(status);
        allowCurrentUser();

        PurchaseOrderDto result = service.startRun("NTL-PO-TEST", "USER-1", null, " งานเร่งด่วน ");

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING, result.getStatus());
        assertNotNull(purchaseOrder.getProductionExpectedEndDate());
        assertEquals("งานเร่งด่วน", result.getStartRunOverrideReason());
        assertEquals("งานเร่งด่วน", purchaseOrder.getStartRunOverrideReason());
        verify(purchaseOrderRepository).saveAndFlush(purchaseOrder);
        verify(purchaseOrderMilestoneService).markJobStarted(purchaseOrder, "USER-1");
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER), eq("NTL-PO-TEST"),
                eq("USER-1"), eq(ActivityActorType.USER), eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                anyString(), historyDetail.capture());
        assertEquals(true, historyDetail.getValue().get("overrideStart"));
        assertEquals(status != PurchaseOrderStatus.CREATED, historyDetail.getValue().get("hasExpectedStatus"));
        assertEquals(false, historyDetail.getValue().get("hasApprovedFirstPayment"));
        assertEquals("งานเร่งด่วน", historyDetail.getValue().get("startRunOverrideReason"));
    }

    @ParameterizedTest
    @EnumSource(PurchaseOrderPaymentScheduleStatus.class)
    void startsWithOverrideForUnpaidOrPartiallyPaidFirstInstallment(PurchaseOrderPaymentScheduleStatus status)
            throws Exception {
        PurchaseOrderPaymentScheduleEntity schedule = new PurchaseOrderPaymentScheduleEntity();
        schedule.setId(1L);
        schedule.setInstallmentNo(1);
        schedule.setExpectedAmount(new BigDecimal("1000"));
        schedule.setStatus(status);
        PurchaseOrderPaymentEntity payment = new PurchaseOrderPaymentEntity();
        payment.setAmount(new BigDecimal(status == PurchaseOrderPaymentScheduleStatus.PAID ? "1000" : "100"));
        payment.setInstallmentNo(1);
        payment.setStatus(status == PurchaseOrderPaymentScheduleStatus.UNPAID
                ? PurchaseOrderPaymentStatus.PENDING : PurchaseOrderPaymentStatus.APPROVED);
        schedule.addPayment(payment);
        schedule.setPaidAmount(status == PurchaseOrderPaymentScheduleStatus.UNPAID
                ? BigDecimal.ZERO : payment.getAmount());
        purchaseOrder.addPaymentSchedule(schedule);
        purchaseOrder.addPayment(payment);
        allowCurrentUser();

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING,
                service.startRun("NTL-PO-TEST", "USER-1", null,
                        status != PurchaseOrderPaymentScheduleStatus.PAID ? "เริ่มงานก่อนชำระเงินครบ" : null).getStatus());

        assertEquals(status, schedule.getStatus());
        verify(purchaseOrderMilestoneService).markJobStarted(purchaseOrder, "USER-1");
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"CANCELLED", "CLOSED"})
    void cancelledAndClosedOrdersStillCannotStart(PurchaseOrderStatus status) {
        purchaseOrder.setStatus(status);

        assertThrows(InvalidRequestException.class, () -> service.startRun("NTL-PO-TEST", "USER-1"));

        assertEquals(status, purchaseOrder.getStatus());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @Test
    void lateStartStillRequiresReason() {
        purchaseOrder.setDocDate(LocalDate.now(DateUtil.getTimeZone()).minusDays(1));

        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", " "));

        assertEquals("lateStartReason is required when starting a late purchase order", error.getMessage());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @Test
    void lateOrderStartsWithReasonEvenWithoutPayment() throws Exception {
        purchaseOrder.setDocDate(LocalDate.now(DateUtil.getTimeZone()).minusDays(1));
        allowCurrentUser();

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING,
                service.startRun("NTL-PO-TEST", "USER-1", " รอเอกสารจากโรงงาน ", "งานเร่งด่วน").getStatus());

        assertEquals("รอเอกสารจากโรงงาน", purchaseOrder.getLateStartReason());
        assertEquals("งานเร่งด่วน", purchaseOrder.getStartRunOverrideReason());
        verify(purchaseOrderMilestoneService).markJobStarted(purchaseOrder, "USER-1");
    }

    @Test
    void repeatedStartDoesNotRestartProduction() throws Exception {
        purchaseOrder.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING);
        purchaseOrder.setStartRunOverrideReason("เหตุผลเดิม");

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING,
                service.startRun("NTL-PO-TEST", "USER-1").getStatus());

        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
        assertEquals("เหตุผลเดิม", purchaseOrder.getStartRunOverrideReason());
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"CREATED", "AWAITING_PAYMENT", "PAID"})
    void startingWithoutApprovedFirstPaymentRequiresOverrideReason(PurchaseOrderStatus status) {
        purchaseOrder.setStatus(status);

        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", null, " "));

        assertEquals("startRunOverrideReason is required when overriding purchase order start conditions",
                error.getMessage());
        assertEquals(status, purchaseOrder.getStatus());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @Test
    void unexpectedStatusRequiresReasonEvenWithApprovedFirstPayment() {
        purchaseOrder.setStatus(PurchaseOrderStatus.CREATED);
        addPayment(1, PurchaseOrderPaymentStatus.APPROVED);

        assertThrows(InvalidRequestException.class, () -> service.startRun("NTL-PO-TEST", "USER-1"));

        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"AWAITING_PAYMENT", "PAID"})
    void expectedStatusWithApprovedFirstPaymentDoesNotRequireOverrideReason(PurchaseOrderStatus status)
            throws Exception {
        purchaseOrder.setStatus(status);
        addPayment(1, PurchaseOrderPaymentStatus.APPROVED);
        allowCurrentUser();

        PurchaseOrderDto result = service.startRun("NTL-PO-TEST", "USER-1");

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING, result.getStatus());
        assertNull(result.getStartRunOverrideReason());
        verify(purchaseOrderMilestoneService).markJobStarted(purchaseOrder, "USER-1");
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderPaymentStatus.class, names = {"PENDING", "REJECTED", "VOIDED"})
    void unapprovedFirstPaymentsStillRequireOverrideReason(PurchaseOrderPaymentStatus status) {
        addPayment(1, status);

        assertThrows(InvalidRequestException.class, () -> service.startRun("NTL-PO-TEST", "USER-1"));

        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @Test
    void approvedSecondInstallmentDoesNotSatisfyFirstInstallmentCondition() {
        addPayment(2, PurchaseOrderPaymentStatus.APPROVED);

        assertThrows(InvalidRequestException.class, () -> service.startRun("NTL-PO-TEST", "USER-1"));

        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @Test
    void lateReasonDoesNotReplaceOverrideReason() {
        purchaseOrder.setDocDate(LocalDate.now(DateUtil.getTimeZone()).minusDays(1));

        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", "รอเอกสารจากโรงงาน", null));

        assertEquals("startRunOverrideReason is required when overriding purchase order start conditions",
                error.getMessage());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void overrideReasonDoesNotReplaceLateReason() {
        purchaseOrder.setDocDate(LocalDate.now(DateUtil.getTimeZone()).minusDays(1));

        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", null, "งานเร่งด่วน"));

        assertEquals("lateStartReason is required when starting a late purchase order", error.getMessage());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void overrideReasonMustFitDatabaseColumn() {
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", null, "x".repeat(2001)));

        assertEquals("startRunOverrideReason must not exceed 2000 characters", error.getMessage());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    private void addPayment(int installmentNo, PurchaseOrderPaymentStatus status) {
        PurchaseOrderPaymentEntity payment = new PurchaseOrderPaymentEntity();
        payment.setInstallmentNo(installmentNo);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("100"));
        purchaseOrder.addPayment(payment);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"AWAITING_PAYMENT", "PAID"})
    void approvedPartialFirstInstallmentRequiresOverrideReason(PurchaseOrderStatus status) {
        purchaseOrder.setStatus(status);
        PurchaseOrderPaymentScheduleEntity schedule = addSchedule(1);
        addScheduledPayment(schedule, "100", PurchaseOrderPaymentStatus.APPROVED);
        recalculateSchedules();

        assertEquals(PurchaseOrderPaymentScheduleStatus.PARTIALLY_PAID, schedule.getStatus());
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.startRun("NTL-PO-TEST", "USER-1", null, " "));

        assertEquals("startRunOverrideReason is required when overriding purchase order start conditions",
                error.getMessage());
        assertEquals(status, purchaseOrder.getStatus());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(purchaseOrderMilestoneService, activityHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"AWAITING_PAYMENT", "PAID"})
    void partialFirstInstallmentStartsWithOverrideAndRecordsReason(PurchaseOrderStatus status) throws Exception {
        purchaseOrder.setStatus(status);
        PurchaseOrderPaymentScheduleEntity schedule = addSchedule(1);
        addScheduledPayment(schedule, "100", PurchaseOrderPaymentStatus.APPROVED);
        recalculateSchedules();
        allowCurrentUser();

        PurchaseOrderDto result = service.startRun("NTL-PO-TEST", "USER-1", null,
                " รันงานก่อนชำระเงินงวดแรกครบ ");

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING, result.getStatus());
        assertEquals("รันงานก่อนชำระเงินงวดแรกครบ", result.getStartRunOverrideReason());
        verify(purchaseOrderMilestoneService).markJobStarted(purchaseOrder, "USER-1");
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER), eq("NTL-PO-TEST"),
                eq("USER-1"), eq(ActivityActorType.USER), eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                anyString(), historyDetail.capture());
        assertEquals(true, historyDetail.getValue().get("overrideStart"));
        assertEquals(true, historyDetail.getValue().get("hasApprovedFirstPayment"));
        assertEquals(true, historyDetail.getValue().get("firstInstallmentPartiallyPaid"));
        assertEquals(PurchaseOrderPaymentScheduleStatus.PARTIALLY_PAID,
                historyDetail.getValue().get("firstInstallmentStatus"));
        assertEquals(schedule.getPaidAmount(), historyDetail.getValue().get("paidAmount"));
    }

    @Test
    void multipleApprovedPaymentsCoveringFirstInstallmentDoNotRequireOverride() throws Exception {
        PurchaseOrderPaymentScheduleEntity schedule = addSchedule(1);
        addScheduledPayment(schedule, "400", PurchaseOrderPaymentStatus.APPROVED);
        addScheduledPayment(schedule, "600", PurchaseOrderPaymentStatus.APPROVED);
        addSchedule(2);
        recalculateSchedules();
        allowCurrentUser();

        assertEquals(PurchaseOrderPaymentScheduleStatus.PAID, schedule.getStatus());
        PurchaseOrderDto result = service.startRun("NTL-PO-TEST", "USER-1");

        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING, result.getStatus());
        assertNull(result.getStartRunOverrideReason());
    }

    @Test
    void pendingRemainingPaymentDoesNotMakeFirstInstallmentFullyPaid() {
        PurchaseOrderPaymentScheduleEntity schedule = addSchedule(1);
        addScheduledPayment(schedule, "100", PurchaseOrderPaymentStatus.APPROVED);
        addScheduledPayment(schedule, "900", PurchaseOrderPaymentStatus.PENDING);
        recalculateSchedules();

        assertEquals(PurchaseOrderPaymentScheduleStatus.PARTIALLY_PAID, schedule.getStatus());
        assertThrows(InvalidRequestException.class, () -> service.startRun("NTL-PO-TEST", "USER-1"));
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void partialSecondInstallmentDoesNotRequireOverrideWhenFirstInstallmentIsPaid() throws Exception {
        PurchaseOrderPaymentScheduleEntity first = addSchedule(1);
        addScheduledPayment(first, "1000", PurchaseOrderPaymentStatus.APPROVED);
        PurchaseOrderPaymentScheduleEntity second = addSchedule(2);
        addScheduledPayment(second, "100", PurchaseOrderPaymentStatus.APPROVED);
        recalculateSchedules();
        allowCurrentUser();

        assertEquals(PurchaseOrderPaymentScheduleStatus.PAID, first.getStatus());
        assertEquals(PurchaseOrderPaymentScheduleStatus.PARTIALLY_PAID, second.getStatus());
        PurchaseOrderDto result = service.startRun("NTL-PO-TEST", "USER-1");
        assertNull(result.getStartRunOverrideReason());
    }

    private PurchaseOrderPaymentScheduleEntity addSchedule(int installmentNo) {
        PurchaseOrderPaymentScheduleEntity schedule = new PurchaseOrderPaymentScheduleEntity();
        schedule.setId((long) installmentNo);
        schedule.setInstallmentNo(installmentNo);
        schedule.setExpectedAmount(new BigDecimal("1000"));
        schedule.setStatus(PurchaseOrderPaymentScheduleStatus.UNPAID);
        purchaseOrder.addPaymentSchedule(schedule);
        return schedule;
    }

    private void addScheduledPayment(PurchaseOrderPaymentScheduleEntity schedule, String amount,
                                     PurchaseOrderPaymentStatus status) {
        PurchaseOrderPaymentEntity payment = new PurchaseOrderPaymentEntity();
        payment.setInstallmentNo(schedule.getInstallmentNo());
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        schedule.addPayment(payment);
        purchaseOrder.addPayment(payment);
    }

    private void recalculateSchedules() {
        new PurchaseOrderPaymentScheduleService(null, null).recalculateSchedules(purchaseOrder);
    }

    private void allowCurrentUser() {
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(new UserEntity()));
    }
}
