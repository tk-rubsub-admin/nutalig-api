package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.dto.PurchaseOrderTimelineDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderMilestoneEntity;
import com.nutalig.entity.PurchaseOrderProofEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderMilestoneRepository;
import com.nutalig.repository.PurchaseOrderProofRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderMilestoneService {

    private static final Set<PurchaseOrderMilestoneCode> PROOF_CODES = Set.of(
            PurchaseOrderMilestoneCode.DIGITAL_PROOF,
            PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK
    );
    private static final Set<PurchaseOrderMilestoneStatus> RESOLVED_PROOF_STATUSES = Set.of(
            PurchaseOrderMilestoneStatus.COMPLETED,
            PurchaseOrderMilestoneStatus.SKIPPED
    );

    private final PurchaseOrderMilestoneRepository milestoneRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderProofRepository proofRepository;
    private final ActivityHistoryService activityHistoryService;

    @Transactional
    public void initializeMilestones(PurchaseOrderEntity purchaseOrder, String userId) {
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones = milestoneMap(purchaseOrder);
        ZonedDateTime now = now();
        List<PurchaseOrderMilestoneEntity> created = new ArrayList<>();
        for (PurchaseOrderMilestoneCode code : PurchaseOrderMilestoneCode.values()) {
            if (milestones.containsKey(code)) continue;
            PurchaseOrderMilestoneEntity milestone = new PurchaseOrderMilestoneEntity();
            milestone.setPurchaseOrder(purchaseOrder);
            milestone.setMilestoneCode(code);
            milestone.setStatus(PurchaseOrderMilestoneStatus.PENDING);
            milestone.setUpdatedBy(userId);
            if (code == PurchaseOrderMilestoneCode.PO_CREATED) {
                milestone.setStatus(PurchaseOrderMilestoneStatus.COMPLETED);
                milestone.setActualAt(Optional.ofNullable(purchaseOrder.getCreatedDate()).orElseGet(() -> {
                    LocalDate documentDate = Optional.ofNullable(purchaseOrder.getDocDate()).orElse(now.toLocalDate());
                    return documentDate.atStartOfDay(DateUtil.getTimeZone());
                }));
            }
            created.add(milestone);
        }
        if (!created.isEmpty()) milestoneRepository.saveAll(created);
    }

    @Transactional
    public void markJobStarted(PurchaseOrderEntity purchaseOrder, String userId) {
        initializeMilestones(purchaseOrder, userId);
        mark(purchaseOrder, PurchaseOrderMilestoneCode.JOB_STARTED,
                PurchaseOrderMilestoneStatus.COMPLETED, null, now(), null, userId);
    }

    @Transactional
    public void markProofStatus(
            PurchaseOrderEntity purchaseOrder,
            String proofTypeCode,
            PurchaseOrderMilestoneStatus status,
            String note,
            String userId
    ) {
        PurchaseOrderMilestoneCode code = proofCode(proofTypeCode);
        if (code == null) return;
        initializeMilestones(purchaseOrder, userId);
        mark(purchaseOrder, code, status, null, now(), note, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void skipProof(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderMilestoneCode code,
            String reason,
            String userId
    ) throws InvalidRequestException {
        if (!PROOF_CODES.contains(code)) {
            throw new InvalidRequestException("Only proof milestones can be skipped.");
        }
        if (StringUtils.isBlank(reason)) {
            throw new InvalidRequestException("note is required when skipping a proof milestone.");
        }
        initializeMilestones(purchaseOrder, userId);
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones = milestoneMap(purchaseOrder);
        requireCompleted(milestones, PurchaseOrderMilestoneCode.JOB_STARTED,
                "Purchase order job has not started.");
        if (code == PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK) {
            requireProofResolved(milestones, PurchaseOrderMilestoneCode.DIGITAL_PROOF,
                    "Digital proof must be approved or skipped first.");
        }
        Optional<PurchaseOrderProofEntity> proof = proofRepository
                .findByPurchaseOrder_PurchaseOrderNoAndProofType_Id_Code(
                        purchaseOrder.getPurchaseOrderNo(), code.name());
        if (proof.isPresent() && (proof.get().getStatus() == PurchaseOrderProofStatus.PENDING_APPROVAL
                || proof.get().getStatus() == PurchaseOrderProofStatus.DRAFT)) {
            throw new InvalidRequestException("Cancel the active proof request before skipping this milestone.");
        }
        PurchaseOrderMilestoneEntity milestone = milestones.get(code);
        if (milestone != null && milestone.getStatus() == PurchaseOrderMilestoneStatus.SKIPPED) {
            return;
        }
        if (milestone != null && milestone.getStatus() == PurchaseOrderMilestoneStatus.COMPLETED) {
            throw new InvalidRequestException("An approved proof cannot be skipped.");
        }
        proof.ifPresent(value -> {
            value.setRequired(Boolean.FALSE);
            value.setStatus(PurchaseOrderProofStatus.CANCELLED);
            value.setUpdatedBy(userId);
            proofRepository.save(value);
        });
        mark(purchaseOrder, code, PurchaseOrderMilestoneStatus.SKIPPED,
                null, now(), reason.trim(), userId);
        record(purchaseOrder, userId, "ข้ามขั้นตอน " + label(code), code, reason.trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeMilestone(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderMilestoneCode code,
            LocalDate plannedDate,
            String note,
            String userId
    ) throws InvalidRequestException {
        initializeMilestones(purchaseOrder, userId);
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED
                || purchaseOrder.getStatus() == PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Purchase order cannot update milestones in status "
                    + purchaseOrder.getStatus());
        }
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones = milestoneMap(purchaseOrder);
        PurchaseOrderMilestoneEntity selectedMilestone = milestones.get(code);
        if (selectedMilestone != null
                && selectedMilestone.getStatus() == PurchaseOrderMilestoneStatus.COMPLETED) {
            return;
        }
        ZonedDateTime actualAt = now();
        LocalDate actualDate = actualAt.toLocalDate();

        switch (code) {
            case PRODUCTION_STARTED -> {
                requireCompleted(milestones, PurchaseOrderMilestoneCode.JOB_STARTED,
                        "Purchase order job has not started.");
                requireProofResolved(milestones, PurchaseOrderMilestoneCode.DIGITAL_PROOF,
                        "Digital proof must be approved or skipped first.");
                requireProofResolved(milestones, PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK,
                        "On-press color check must be approved or skipped first.");
                LocalDate expectedEnd = plannedDate != null
                        ? plannedDate
                        : Optional.ofNullable(purchaseOrder.getProductionExpectedEndDate())
                        .orElseGet(() -> actualDate.plusDays(Math.max(0,
                                Optional.ofNullable(purchaseOrder.getProductionLeadTimeDay()).orElse(0))));
                if (expectedEnd.isBefore(actualDate)) {
                    throw new InvalidRequestException("plannedDate cannot be before the production start date.");
                }
                purchaseOrder.setProductionStartedDate(actualDate);
                purchaseOrder.setProductionExpectedEndDate(expectedEnd);
                mark(purchaseOrder, PurchaseOrderMilestoneCode.PRODUCTION_STARTED,
                        PurchaseOrderMilestoneStatus.COMPLETED, null, actualAt, note, userId);
                mark(purchaseOrder, PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END,
                        PurchaseOrderMilestoneStatus.COMPLETED, expectedEnd, actualAt, null, userId);
            }
            case PRODUCTION_COMPLETED -> {
                requireCompleted(milestones, PurchaseOrderMilestoneCode.PRODUCTION_STARTED,
                        "Production has not started.");
                requireProofResolved(milestones, PurchaseOrderMilestoneCode.DIGITAL_PROOF,
                        "Digital proof must be approved or skipped first.");
                requireProofResolved(milestones, PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK,
                        "On-press color check must be approved or skipped first.");
                purchaseOrder.setProductionCompletedDate(actualDate);
                mark(purchaseOrder, PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED,
                        PurchaseOrderMilestoneStatus.COMPLETED, null, actualAt, note, userId);
            }
            case ARRIVED_AT_CARRIER -> {
                requireCompleted(milestones, PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED,
                        "Production must be completed first.");
                mark(purchaseOrder, code, PurchaseOrderMilestoneStatus.COMPLETED,
                        null, actualAt, note, userId);
            }
            case IN_TRANSIT -> {
                requireCompleted(milestones, PurchaseOrderMilestoneCode.ARRIVED_AT_CARRIER,
                        "Goods must arrive at the carrier first.");
                mark(purchaseOrder, code, PurchaseOrderMilestoneStatus.COMPLETED,
                        null, actualAt, note, userId);
            }
            case WAREHOUSE_RECEIVED -> {
                requireCompleted(milestones, PurchaseOrderMilestoneCode.IN_TRANSIT,
                        "Goods must be in transit first.");
                mark(purchaseOrder, code, PurchaseOrderMilestoneStatus.COMPLETED,
                        null, actualAt, note, userId);
            }
            default -> throw new InvalidRequestException("Milestone " + code + " cannot be completed manually.");
        }

        purchaseOrderRepository.saveAndFlush(purchaseOrder);
        record(purchaseOrder, userId, "บันทึกขั้นตอน " + label(code), code, note);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProductionExpectedEndDate(
            PurchaseOrderEntity purchaseOrder,
            LocalDate expectedEndDate,
            String userId
    ) throws InvalidRequestException {
        if (expectedEndDate == null) {
            throw new InvalidRequestException("plannedDate is required.");
        }
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED
                || purchaseOrder.getStatus() == PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Purchase order cannot update production expected end date in status "
                    + purchaseOrder.getStatus());
        }

        initializeMilestones(purchaseOrder, userId);
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones = milestoneMap(purchaseOrder);
        requireCompleted(milestones, PurchaseOrderMilestoneCode.JOB_STARTED,
                "Purchase order job has not started.");
        if (purchaseOrder.getProductionCompletedDate() != null
                || Optional.ofNullable(milestones.get(PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED))
                .map(PurchaseOrderMilestoneEntity::getStatus)
                .filter(PurchaseOrderMilestoneStatus.COMPLETED::equals)
                .isPresent()) {
            throw new InvalidRequestException("Production has already been completed.");
        }
        if (purchaseOrder.getProductionStartedDate() != null
                && expectedEndDate.isBefore(purchaseOrder.getProductionStartedDate())) {
            throw new InvalidRequestException("plannedDate cannot be before the production start date.");
        }

        LocalDate previousExpectedEndDate = purchaseOrder.getProductionExpectedEndDate();
        if (Objects.equals(previousExpectedEndDate, expectedEndDate)) {
            return;
        }

        purchaseOrder.setProductionExpectedEndDate(expectedEndDate);
        mark(purchaseOrder, PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END,
                PurchaseOrderMilestoneStatus.COMPLETED, expectedEndDate, now(), null, userId);
        purchaseOrderRepository.saveAndFlush(purchaseOrder);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("milestoneCode", PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END.name());
        detail.put("previousProductionExpectedEndDate", previousExpectedEndDate);
        detail.put("productionExpectedEndDate", expectedEndDate);
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                purchaseOrder.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "เปลี่ยนกำหนดวันผลิตเสร็จสำหรับใบสั่งซื้อเลขที่ "
                        + purchaseOrder.getPurchaseOrderNo(),
                detail
        );
    }

    @Transactional
    public void cancelRemaining(PurchaseOrderEntity purchaseOrder, String userId) {
        initializeMilestones(purchaseOrder, userId);
        List<PurchaseOrderMilestoneEntity> milestones = milestoneRepository
                .findAllByPurchaseOrder_PurchaseOrderNo(purchaseOrder.getPurchaseOrderNo());
        for (PurchaseOrderMilestoneEntity milestone : milestones) {
            if (!isTerminal(milestone.getStatus())) {
                milestone.setStatus(PurchaseOrderMilestoneStatus.CANCELLED);
                milestone.setActualAt(now());
                milestone.setUpdatedBy(userId);
            }
        }
        milestoneRepository.saveAll(milestones);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderTimelineDto.Event> getTimelineEvents(PurchaseOrderEntity purchaseOrder) {
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones = milestoneMap(purchaseOrder);
        Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneStatus> statuses = new EnumMap<>(PurchaseOrderMilestoneCode.class);
        for (PurchaseOrderMilestoneCode code : PurchaseOrderMilestoneCode.values()) {
            statuses.put(code, resolvedStatus(purchaseOrder, code, milestones.get(code)));
        }

        List<PurchaseOrderTimelineDto.Event> events = new ArrayList<>();
        for (PurchaseOrderMilestoneCode code : PurchaseOrderMilestoneCode.values()) {
            PurchaseOrderMilestoneEntity milestone = milestones.get(code);
            PurchaseOrderMilestoneStatus status = statuses.get(code);
            PurchaseOrderTimelineDto.Event event = new PurchaseOrderTimelineDto.Event();
            event.setType(code.name());
            event.setLabel(label(code));
            event.setStatus(status);
            event.setCompleted(isResolved(status));
            event.setOptional(PROOF_CODES.contains(code));
            event.setPlannedDate(milestone != null ? milestone.getPlannedDate() : fallbackPlannedDate(purchaseOrder, code));
            event.setActualAt(milestone != null ? milestone.getActualAt() : fallbackActualAt(purchaseOrder, code));
            event.setDate(code == PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END
                    ? event.getPlannedDate()
                    : event.getActualAt() != null
                    ? event.getActualAt().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate()
                    : event.getPlannedDate());
            event.setNote(milestone != null ? milestone.getNote() : null);
            event.setOverdue(code == PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END
                    && event.getPlannedDate() != null
                    && statuses.get(PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED)
                    != PurchaseOrderMilestoneStatus.COMPLETED
                    && LocalDate.now(DateUtil.getTimeZone()).isAfter(event.getPlannedDate()));
            event.setCanSkip(canSkip(code, status, statuses));
            event.setCanComplete(canComplete(code, status, statuses));
            events.add(event);
        }
        return events;
    }

    @Transactional(readOnly = true)
    public boolean isProofResolved(PurchaseOrderEntity purchaseOrder, PurchaseOrderMilestoneCode code) {
        if (!PROOF_CODES.contains(code)) return false;
        PurchaseOrderMilestoneEntity milestone = milestoneRepository
                .findByPurchaseOrder_PurchaseOrderNoAndMilestoneCode(
                        purchaseOrder.getPurchaseOrderNo(), code
                ).orElse(null);
        return milestone != null && RESOLVED_PROOF_STATUSES.contains(milestone.getStatus());
    }

    @Transactional(readOnly = true)
    public boolean isProofSkipped(PurchaseOrderEntity purchaseOrder, PurchaseOrderMilestoneCode code) {
        if (!PROOF_CODES.contains(code)) return false;
        return milestoneRepository.findByPurchaseOrder_PurchaseOrderNoAndMilestoneCode(
                        purchaseOrder.getPurchaseOrderNo(), code
                )
                .map(PurchaseOrderMilestoneEntity::getStatus)
                .filter(PurchaseOrderMilestoneStatus.SKIPPED::equals)
                .isPresent();
    }

    private boolean canSkip(
            PurchaseOrderMilestoneCode code,
            PurchaseOrderMilestoneStatus status,
            Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneStatus> statuses
    ) {
        if (!PROOF_CODES.contains(code) || isResolved(status)) return false;
        if (status != PurchaseOrderMilestoneStatus.PENDING
                && status != PurchaseOrderMilestoneStatus.CHANGES_REQUESTED
                && status != PurchaseOrderMilestoneStatus.CANCELLED) return false;
        if (statuses.get(PurchaseOrderMilestoneCode.JOB_STARTED) != PurchaseOrderMilestoneStatus.COMPLETED) return false;
        return code != PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK
                || isResolved(statuses.get(PurchaseOrderMilestoneCode.DIGITAL_PROOF));
    }

    private boolean canComplete(
            PurchaseOrderMilestoneCode code,
            PurchaseOrderMilestoneStatus status,
            Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneStatus> statuses
    ) {
        if (status == PurchaseOrderMilestoneStatus.COMPLETED
                || status == PurchaseOrderMilestoneStatus.SKIPPED
                || status == PurchaseOrderMilestoneStatus.CANCELLED) return false;
        return switch (code) {
            case PRODUCTION_STARTED -> statuses.get(PurchaseOrderMilestoneCode.JOB_STARTED)
                    == PurchaseOrderMilestoneStatus.COMPLETED
                    && isResolved(statuses.get(PurchaseOrderMilestoneCode.DIGITAL_PROOF))
                    && isResolved(statuses.get(PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK));
            case PRODUCTION_COMPLETED -> statuses.get(PurchaseOrderMilestoneCode.PRODUCTION_STARTED)
                    == PurchaseOrderMilestoneStatus.COMPLETED
                    && isResolved(statuses.get(PurchaseOrderMilestoneCode.DIGITAL_PROOF))
                    && isResolved(statuses.get(PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK));
            case ARRIVED_AT_CARRIER -> statuses.get(PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED)
                    == PurchaseOrderMilestoneStatus.COMPLETED;
            case IN_TRANSIT -> statuses.get(PurchaseOrderMilestoneCode.ARRIVED_AT_CARRIER)
                    == PurchaseOrderMilestoneStatus.COMPLETED;
            case WAREHOUSE_RECEIVED -> statuses.get(PurchaseOrderMilestoneCode.IN_TRANSIT)
                    == PurchaseOrderMilestoneStatus.COMPLETED;
            default -> false;
        };
    }

    private PurchaseOrderMilestoneStatus resolvedStatus(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderMilestoneCode code,
            PurchaseOrderMilestoneEntity milestone
    ) {
        if (milestone != null) return milestone.getStatus();
        if (code == PurchaseOrderMilestoneCode.PO_CREATED) return PurchaseOrderMilestoneStatus.COMPLETED;
        if ((code == PurchaseOrderMilestoneCode.JOB_STARTED || code == PurchaseOrderMilestoneCode.PRODUCTION_STARTED)
                && purchaseOrder.getProductionStartedDate() != null) return PurchaseOrderMilestoneStatus.COMPLETED;
        if (code == PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END
                && purchaseOrder.getProductionExpectedEndDate() != null) {
            return PurchaseOrderMilestoneStatus.COMPLETED;
        }
        if (code == PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED
                && purchaseOrder.getProductionCompletedDate() != null) return PurchaseOrderMilestoneStatus.COMPLETED;
        return purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED
                ? PurchaseOrderMilestoneStatus.CANCELLED : PurchaseOrderMilestoneStatus.PENDING;
    }

    private ZonedDateTime fallbackActualAt(PurchaseOrderEntity purchaseOrder, PurchaseOrderMilestoneCode code) {
        LocalDate date = switch (code) {
            case PO_CREATED -> purchaseOrder.getDocDate();
            case JOB_STARTED, PRODUCTION_STARTED -> purchaseOrder.getProductionStartedDate();
            case PRODUCTION_COMPLETED -> purchaseOrder.getProductionCompletedDate();
            default -> null;
        };
        return date != null ? date.atStartOfDay(DateUtil.getTimeZone()) : null;
    }

    private LocalDate fallbackPlannedDate(PurchaseOrderEntity purchaseOrder, PurchaseOrderMilestoneCode code) {
        return code == PurchaseOrderMilestoneCode.PRODUCTION_EXPECTED_END
                ? purchaseOrder.getProductionExpectedEndDate() : null;
    }

    private Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestoneMap(PurchaseOrderEntity purchaseOrder) {
        List<PurchaseOrderMilestoneEntity> records = milestoneRepository
                .findAllByPurchaseOrder_PurchaseOrderNo(purchaseOrder.getPurchaseOrderNo());
        if (records == null) return new EnumMap<>(PurchaseOrderMilestoneCode.class);
        return records.stream().collect(Collectors.toMap(
                PurchaseOrderMilestoneEntity::getMilestoneCode,
                Function.identity(),
                (first, ignored) -> first,
                () -> new EnumMap<>(PurchaseOrderMilestoneCode.class)
        ));
    }

    private void mark(
            PurchaseOrderEntity purchaseOrder,
            PurchaseOrderMilestoneCode code,
            PurchaseOrderMilestoneStatus status,
            LocalDate plannedDate,
            ZonedDateTime actualAt,
            String note,
            String userId
    ) {
        PurchaseOrderMilestoneEntity milestone = milestoneRepository
                .findByPurchaseOrder_PurchaseOrderNoAndMilestoneCode(purchaseOrder.getPurchaseOrderNo(), code)
                .orElseGet(() -> {
                    PurchaseOrderMilestoneEntity value = new PurchaseOrderMilestoneEntity();
                    value.setPurchaseOrder(purchaseOrder);
                    value.setMilestoneCode(code);
                    return value;
                });
        milestone.setStatus(status);
        milestone.setPlannedDate(plannedDate);
        milestone.setActualAt(actualAt);
        milestone.setNote(StringUtils.trimToNull(note));
        milestone.setUpdatedBy(userId);
        milestoneRepository.save(milestone);
    }

    private void requireCompleted(
            Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones,
            PurchaseOrderMilestoneCode code,
            String message
    ) throws InvalidRequestException {
        PurchaseOrderMilestoneEntity milestone = milestones.get(code);
        if (milestone == null || milestone.getStatus() != PurchaseOrderMilestoneStatus.COMPLETED) {
            throw new InvalidRequestException(message);
        }
    }

    private void requireProofResolved(
            Map<PurchaseOrderMilestoneCode, PurchaseOrderMilestoneEntity> milestones,
            PurchaseOrderMilestoneCode code,
            String message
    ) throws InvalidRequestException {
        PurchaseOrderMilestoneEntity milestone = milestones.get(code);
        if (milestone == null || !RESOLVED_PROOF_STATUSES.contains(milestone.getStatus())) {
            throw new InvalidRequestException(message);
        }
    }

    private boolean isResolved(PurchaseOrderMilestoneStatus status) {
        return status == PurchaseOrderMilestoneStatus.COMPLETED
                || status == PurchaseOrderMilestoneStatus.SKIPPED;
    }

    private boolean isTerminal(PurchaseOrderMilestoneStatus status) {
        return isResolved(status) || status == PurchaseOrderMilestoneStatus.CANCELLED;
    }

    private PurchaseOrderMilestoneCode proofCode(String proofTypeCode) {
        if (StringUtils.equalsIgnoreCase(proofTypeCode, PurchaseOrderMilestoneCode.DIGITAL_PROOF.name())) {
            return PurchaseOrderMilestoneCode.DIGITAL_PROOF;
        }
        if (StringUtils.equalsIgnoreCase(proofTypeCode, PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK.name())) {
            return PurchaseOrderMilestoneCode.ON_PRESS_COLOR_CHECK;
        }
        return null;
    }

    private String label(PurchaseOrderMilestoneCode code) {
        return switch (code) {
            case PO_CREATED -> "สร้างใบสั่งซื้อ";
            case JOB_STARTED -> "เริ่มรันงาน";
            case DIGITAL_PROOF -> "พรูฟดิจิตอลปริ้นท์";
            case ON_PRESS_COLOR_CHECK -> "พรูฟสีหน้าเครื่อง";
            case PRODUCTION_STARTED -> "เริ่มผลิต";
            case PRODUCTION_EXPECTED_END -> "กำหนดผลิตเสร็จ";
            case PRODUCTION_COMPLETED -> "ผลิตเสร็จ";
            case ARRIVED_AT_CARRIER -> "สินค้าถึงขนส่ง";
            case IN_TRANSIT -> "อยู่ระหว่างขนส่ง";
            case WAREHOUSE_RECEIVED -> "ถึงคลังสินค้า";
        };
    }

    private void record(
            PurchaseOrderEntity purchaseOrder,
            String userId,
            String title,
            PurchaseOrderMilestoneCode code,
            String note
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("milestoneCode", code.name());
        detail.put("note", StringUtils.trimToNull(note));
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                purchaseOrder.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                title + " สำหรับใบสั่งซื้อเลขที่ " + purchaseOrder.getPurchaseOrderNo(),
                detail
        );
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(DateUtil.getTimeZone());
    }
}
