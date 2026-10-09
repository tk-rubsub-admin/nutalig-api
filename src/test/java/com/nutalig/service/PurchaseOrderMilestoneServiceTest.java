package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderMilestoneEntity;
import com.nutalig.entity.PurchaseOrderProofEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderMilestoneRepository;
import com.nutalig.repository.PurchaseOrderProofRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.utils.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderMilestoneServiceTest {
    @Mock PurchaseOrderMilestoneRepository milestoneRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderProofRepository proofRepository;
    @Mock ActivityHistoryService activityHistoryService;
    @InjectMocks PurchaseOrderMilestoneService service;

    private PurchaseOrderEntity po;
    private final Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones =
            new EnumMap<>(PurchaseOrderMilestoneCode.class);

    @BeforeEach
    void setup() {
        milestones.clear();
        po = new PurchaseOrderEntity();
        po.setPurchaseOrderNo("PO-AUTO-PRODUCTION");
        po.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING);
        po.setProductionLeadTimeDay(35);
        milestone(PurchaseOrderMilestoneCode.JOB_STARTED, PurchaseOrderMilestoneStatus.COMPLETED);
        lenient().when(purchaseOrderRepository.findByIdForUpdate(po.getPurchaseOrderNo()))
                .thenReturn(Optional.of(po));
        when(milestoneRepository.findAllByPurchaseOrder_PurchaseOrderNo(po.getPurchaseOrderNo()))
                .thenAnswer(invocation -> new ArrayList<>(milestones.values()));
        lenient().when(milestoneRepository.findByPurchaseOrder_PurchaseOrderNoAndMilestoneCode(
                        eq(po.getPurchaseOrderNo()), any()))
                .thenAnswer(invocation -> Optional.ofNullable(milestones.get(invocation.getArgument(1))));
        lenient().when(milestoneRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<PurchaseOrderMilestoneEntity> entities = invocation.getArgument(0);
            entities.forEach(entity -> milestones.put(entity.getMilestoneCode(), entity));
            return new ArrayList<>(milestones.values());
        });
        lenient().when(milestoneRepository.save(any())).thenAnswer(invocation -> {
            PurchaseOrderMilestoneEntity entity = invocation.getArgument(0);
            milestones.put(entity.getMilestoneCode(), entity);
            return entity;
        });
    }

    private PurchaseOrderMilestoneEntity milestone(
            PurchaseOrderMilestoneCode code, PurchaseOrderMilestoneStatus status
    ) {
        PurchaseOrderMilestoneEntity entity = new PurchaseOrderMilestoneEntity();
        entity.setPurchaseOrder(po);
        entity.setMilestoneCode(code);
        entity.setStatus(status);
        milestones.put(code, entity);
        return entity;
    }

    private void resolveProof(PurchaseOrderMilestoneCode code, String action) throws Exception {
        if ("SKIP".equals(action)) {
            service.skipProof(po, code, "ไม่ต้องพรูฟขั้นตอนนี้", "USER-1");
        } else {
            service.markProofStatus(po, code.name(), PurchaseOrderMilestoneStatus.COMPLETED,
                    "อนุมัติแล้ว", "USER-1");
        }
    }

    @ParameterizedTest
    @CsvSource({"APPROVE,APPROVE", "APPROVE,SKIP", "SKIP,APPROVE", "SKIP,SKIP"})
    void startsAutomaticallyWhenBothProofsResolve(String digitalAction, String onPressAction) throws Exception {
        resolveProof(PurchaseOrderMilestoneCode.DIGITAL_PROOF, digitalAction);
        assertNull(po.getProductionStartedDate(), "One resolved proof must not start production");
        verify(purchaseOrderRepository, never()).saveAndFlush(any());

        resolveProof(PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK, onPressAction);

        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        assertEquals(today, po.getProductionStartedDate());
        assertEquals(today.plusDays(35), po.getProductionExpectedEndDate());
        assertNull(po.getProductionCompletedDate());
        assertEquals(PurchaseOrderStatus.PRODUCTION_RUNNING, po.getStatus());
        var started = milestones.get(PurchaseOrderMilestoneCode.PRODUCTION_STARTED);
        assertEquals(PurchaseOrderMilestoneStatus.COMPLETED, started.getStatus());
        assertEquals(today, started.getActualAt().toLocalDate());
        assertEquals("USER-1", started.getUpdatedBy());
        assertTrue(started.getNote().contains("เริ่มผลิตอัตโนมัติ"));
        var expected = milestones.get(PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END);
        assertEquals(PurchaseOrderMilestoneStatus.COMPLETED, expected.getStatus());
        assertEquals(today.plusDays(35), expected.getPlannedDate());
        verify(purchaseOrderRepository).saveAndFlush(po);
        verify(purchaseOrderRepository, times(2)).findByIdForUpdate(po.getPurchaseOrderNo());
        verifyAutomaticHistoryOnce();
    }

    private void verifyAutomaticHistoryOnce() {
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER),
                eq(po.getPurchaseOrderNo()), eq("USER-1"), eq(ActivityActorType.USER),
                eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                startsWith("เริ่มผลิตอัตโนมัติ"), anyMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 5})
    void preservesTheExistingScheduleIncludingAnOverdueDate(int daysFromToday) {
        LocalDate planned = LocalDate.now(DateUtil.getTimeZone()).plusDays(daysFromToday);
        po.setProductionExpectedEndDate(planned);
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.COMPLETED);

        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");

        assertNotNull(po.getProductionStartedDate());
        assertEquals(planned, po.getProductionExpectedEndDate());
        assertEquals(planned, milestones.get(PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END).getPlannedDate());
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderMilestoneStatus.class,
            names = {"PENDING", "IN_PROGRESS", "CHANGES_REQUESTED", "CANCELLED"})
    void doesNotStartIfTheOtherProofIsUnresolved(PurchaseOrderMilestoneStatus digitalStatus) {
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, digitalStatus);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(activityHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = "PRODUCTION_RUNNING")
    void doesNotStartOutsideProductionRunning(PurchaseOrderStatus status) {
        po.setStatus(status);
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.COMPLETED);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void doesNotStartBeforeTheJobHasStarted() {
        milestone(PurchaseOrderMilestoneCode.JOB_STARTED, PurchaseOrderMilestoneStatus.PENDING);
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void repeatedApprovalDoesNotResetProductionDatesOrRecordAnotherStart() {
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        LocalDate started = po.getProductionStartedDate();
        LocalDate expected = po.getProductionExpectedEndDate();
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertEquals(started, po.getProductionStartedDate());
        assertEquals(expected, po.getProductionExpectedEndDate());
        verify(purchaseOrderRepository).saveAndFlush(po);
        verifyAutomaticHistoryOnce();
    }

    @Test
    void doesNotRestartCompletedProduction() {
        po.setProductionCompletedDate(LocalDate.now(DateUtil.getTimeZone()));
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderMilestoneStatus.class, names = {"COMPLETED", "CANCELLED", "SKIPPED"})
    void doesNotReopenATerminalProductionMilestone(PurchaseOrderMilestoneStatus status) {
        milestone(PurchaseOrderMilestoneCode.PRODUCTION_STARTED, status);
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        service.markProofStatus(po, "ON_PRESS_COLOR_CHECK", PurchaseOrderMilestoneStatus.COMPLETED,
                null, "USER-1");
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void skipsAnAlreadySkippedProofIdempotentlyAndCanStartALegacyOrder() throws Exception {
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        milestone(PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK, PurchaseOrderMilestoneStatus.SKIPPED);
        service.skipProof(po, PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK, "ไม่ต้องพรูฟ", "USER-1");
        assertNotNull(po.getProductionStartedDate());
        verifyAutomaticHistoryOnce();
    }

    @Test
    void anActiveProofCannotBeSkippedToTriggerAutomaticProduction() {
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.COMPLETED);
        PurchaseOrderProofEntity proof = new PurchaseOrderProofEntity();
        proof.setStatus(PurchaseOrderProofStatus.PENDING_APPROVAL);
        when(proofRepository.findByPurchaseOrder_PurchaseOrderNoAndProofType_Id_Code(
                po.getPurchaseOrderNo(), "ON_PRESS_COLOR_CHECK"))
                .thenReturn(Optional.of(proof));
        assertThrows(InvalidRequestException.class, () -> service.skipProof(po,
                PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK, "ไม่ต้องพรูฟ", "USER-1"));
        assertNull(po.getProductionStartedDate());
        verify(purchaseOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void manualProductionStartStillUsesTheChosenDate() throws Exception {
        milestone(PurchaseOrderMilestoneCode.DIGITAL_PROOF, PurchaseOrderMilestoneStatus.SKIPPED);
        milestone(PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK, PurchaseOrderMilestoneStatus.COMPLETED);
        LocalDate expected = LocalDate.now(DateUtil.getTimeZone()).plusDays(20);
        service.completeMilestone(po, PurchaseOrderMilestoneCode.PRODUCTION_STARTED,
                expected, "เริ่มผลิตด้วยตนเอง", "USER-1");
        assertNotNull(po.getProductionStartedDate());
        assertEquals(expected, po.getProductionExpectedEndDate());
        assertEquals("เริ่มผลิตด้วยตนเอง", milestones.get(PurchaseOrderMilestoneCode.PRODUCTION_STARTED).getNote());
    }
}
