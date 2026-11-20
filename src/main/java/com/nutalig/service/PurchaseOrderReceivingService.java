package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.PurchaseOrderReceiptStatus;
import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderReceiptRequest;
import com.nutalig.dto.PurchaseOrderReceiptDetailDto;
import com.nutalig.dto.PurchaseOrderReceiptDto;
import com.nutalig.entity.PurchaseOrderDeliveryDetailEntity;
import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderReceiptDetailEntity;
import com.nutalig.entity.PurchaseOrderReceiptEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import com.nutalig.repository.PurchaseOrderReceiptRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nutalig.constant.BusinessConstant.DocumentPrefix.GOODS_RECEIPT_PREFIX;

@Service
@RequiredArgsConstructor
public class PurchaseOrderReceivingService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDeliveryRepository deliveryRepository;
    private final PurchaseOrderReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final PurchaseOrderStatusService purchaseOrderStatusService;
    private final ActivityHistoryService activityHistoryService;
    private final PurchaseOrderReceiptStockMovementService stockMovementService;

    @Transactional(readOnly = true)
    public PurchaseOrderReceiptDto getReceiveContext(String purchaseOrderNo, Long deliveryId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderDeliveryEntity delivery = findDelivery(purchaseOrderNo, deliveryId);
        if (delivery.getStatus() != PurchaseOrderDeliveryStatus.IN_TRANSIT
                && delivery.getStatus() != PurchaseOrderDeliveryStatus.PARTIALLY_RECEIVED) {
            throw new InvalidRequestException("Delivery must be in transit or partially received.");
        }
        PurchaseOrderReceiptDto dto = baseDto(delivery);
        dto.setDetails(buildContextDetails(delivery));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderReceiptDto> getReceipts(String purchaseOrderNo, Long deliveryId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderDeliveryEntity delivery = findDelivery(purchaseOrderNo, deliveryId);
        return receiptRepository.findByDelivery_IdOrderByCreatedDateDesc(deliveryId).stream()
                .map(receipt -> mapReceipt(receipt, delivery))
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderReceiptDto getReceipt(String receiptNo) throws DataNotFoundException {
        PurchaseOrderReceiptEntity receipt = receiptRepository.findById(receiptNo)
                .orElseThrow(() -> new DataNotFoundException("Receipt " + receiptNo + " not found."));
        return mapReceipt(receipt, receipt.getDelivery());
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderReceiptDto createReceipt(
            String purchaseOrderNo, Long deliveryId, CreatePurchaseOrderReceiptRequest request, String userId
    ) throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderDeliveryEntity delivery = findDeliveryForUpdate(purchaseOrderNo, deliveryId);
        if (delivery.getStatus() != PurchaseOrderDeliveryStatus.IN_TRANSIT
                && delivery.getStatus() != PurchaseOrderDeliveryStatus.PARTIALLY_RECEIVED) {
            throw new InvalidRequestException("Delivery must be in transit or partially received.");
        }
        if (request == null || request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new InvalidRequestException("At least one receipt detail is required.");
        }
        if (request.getWarehouseId() == null || request.getWarehouseId() <= 0) {
            throw new InvalidRequestException("warehouseId is required.");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        Map<Long, PurchaseOrderDeliveryDetailEntity> deliveryDetails = delivery.getDetails().stream()
                .filter(detail -> detail.getId() != null)
                .collect(Collectors.toMap(PurchaseOrderDeliveryDetailEntity::getId, Function.identity()));
        Map<Long, Totals> previous = previousTotals(delivery);
        Set<Long> selectedIds = new HashSet<>();
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        PurchaseOrderReceiptEntity receipt = new PurchaseOrderReceiptEntity();
        receipt.setReceiptNo(generatedIdSequenceService.getNextIdWithMonth(GOODS_RECEIPT_PREFIX, 5));
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setDelivery(delivery);
        receipt.setWarehouseId(request.getWarehouseId());
        receipt.setReceivedDate(Optional.ofNullable(request.getReceivedDate()).orElse(LocalDate.now(DateUtil.getTimeZone())));
        receipt.setReceivedBy(user);
        receipt.setStatus(PurchaseOrderReceiptStatus.CONFIRMED);
        receipt.setRemark(StringUtils.trimToNull(request.getRemark()));
        receipt.setCreatedDate(now);
        receipt.setUpdatedDate(now);

        for (CreatePurchaseOrderReceiptRequest.Detail input : request.getDetails()) {
            if (input == null || input.getDeliveryDetailId() == null) {
                throw new InvalidRequestException("deliveryDetailId is required.");
            }
            if (!selectedIds.add(input.getDeliveryDetailId())) {
                throw new InvalidRequestException("Duplicate deliveryDetailId in receipt request.");
            }
            PurchaseOrderDeliveryDetailEntity deliveryDetail = deliveryDetails.get(input.getDeliveryDetailId());
            if (deliveryDetail == null) {
                throw new InvalidRequestException("Receipt detail does not belong to this delivery.");
            }
            BigDecimal received = nonNegative(input.getReceivedQuantity(), "receivedQuantity");
            BigDecimal damaged = nonNegative(input.getDamagedQuantity(), "damagedQuantity");
            BigDecimal shortage = nonNegative(input.getShortageQuantity(), "shortageQuantity");
            BigDecimal excess = nonNegative(input.getExcessQuantity(), "excessQuantity");
            BigDecimal shipped = valueOrZero(deliveryDetail.getQuantity());
            BigDecimal alreadyHandled = previous.getOrDefault(input.getDeliveryDetailId(), Totals.ZERO).handled();
            BigDecimal remaining = shipped.subtract(alreadyHandled).max(BigDecimal.ZERO);
            if (received.add(damaged).add(shortage).compareTo(remaining) > 0) {
                throw new InvalidRequestException("Receipt quantity exceeds the remaining delivery quantity.");
            }
            if (received.add(damaged).add(shortage).add(excess).signum() <= 0) {
                throw new InvalidRequestException("Receipt detail must have a quantity.");
            }
            if (input.getExpiryDate() != null && input.getExpiryDate().isBefore(receipt.getReceivedDate())) {
                throw new InvalidRequestException("expiryDate cannot be before receivedDate.");
            }
            if (received.signum() > 0 && (input.getWarehouseLocationId() == null || input.getWarehouseLocationId() <= 0)) {
                throw new InvalidRequestException("warehouseLocationId is required for received quantity.");
            }
            if ((damaged.signum() > 0 || shortage.signum() > 0 || excess.signum() > 0)
                    && StringUtils.isBlank(input.getRemark()) && StringUtils.isBlank(request.getRemark())) {
                throw new InvalidRequestException("remark is required when receipt has an issue.");
            }
            PurchaseOrderReceiptDetailEntity detail = new PurchaseOrderReceiptDetailEntity();
            detail.setDeliveryDetail(deliveryDetail);
            detail.setShippedQuantity(shipped);
            detail.setReceivedQuantity(received);
            detail.setDamagedQuantity(damaged);
            detail.setShortageQuantity(shortage);
            detail.setExcessQuantity(excess);
            detail.setLotNo(StringUtils.trimToNull(input.getLotNo()));
            detail.setExpiryDate(input.getExpiryDate());
            detail.setWarehouseLocationId(input.getWarehouseLocationId());
            detail.setRemark(StringUtils.trimToNull(input.getRemark()));
            receipt.addDetail(detail);
        }

        receiptRepository.save(receipt);
        stockMovementService.recordReceipt(receipt, userId);
        refreshDeliveryStatus(delivery);
        purchaseOrderStatusService.refresh(purchaseOrder);
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                purchaseOrderNo,
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างเอกสารรับสินค้า " + receipt.getReceiptNo(),
                buildAuditDetail(receipt, "NONE", PurchaseOrderReceiptStatus.CONFIRMED.name(), delivery.getStatus().name())
        );
        return mapReceipt(receipt, delivery);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderReceiptDto cancelReceipt(String receiptNo, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderReceiptEntity receipt = receiptRepository
                .findById(receiptNo)
                .orElseThrow(() -> new DataNotFoundException("Receipt " + receiptNo + " not found."));
        if (receipt.getStatus() != PurchaseOrderReceiptStatus.CONFIRMED) {
            throw new InvalidRequestException("Only confirmed receipt can be cancelled.");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        receipt.setStatus(PurchaseOrderReceiptStatus.CANCELLED);
        receipt.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        receiptRepository.save(receipt);
        stockMovementService.reverseReceipt(receipt, userId);
        refreshDeliveryStatus(receipt.getDelivery());
        purchaseOrderStatusService.refresh(receipt.getPurchaseOrder());
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                receipt.getPurchaseOrder().getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                "ยกเลิกเอกสารรับสินค้า " + receiptNo,
                buildAuditDetail(receipt, PurchaseOrderReceiptStatus.CONFIRMED.name(), PurchaseOrderReceiptStatus.CANCELLED.name(), receipt.getDelivery().getStatus().name())
        );
        return mapReceipt(receipt, receipt.getDelivery());
    }

    private Map<String, Object> buildAuditDetail(PurchaseOrderReceiptEntity receipt, String statusBefore,
                                                   String statusAfter, String deliveryStatusAfter) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("receiptNo", receipt.getReceiptNo());
        detail.put("deliveryId", receipt.getDelivery().getId());
        detail.put("statusBefore", statusBefore);
        detail.put("statusAfter", statusAfter);
        detail.put("deliveryStatusAfter", deliveryStatusAfter);
        detail.put("warehouseId", receipt.getWarehouseId());
        detail.put("receivedDate", receipt.getReceivedDate());
        detail.put("remark", receipt.getRemark());
        detail.put("details", receipt.getDetails().stream().map(detailEntity -> Map.of(
                "deliveryDetailId", detailEntity.getDeliveryDetail().getId(),
                "shippedQuantity", valueOrZero(detailEntity.getShippedQuantity()),
                "receivedQuantity", valueOrZero(detailEntity.getReceivedQuantity()),
                "damagedQuantity", valueOrZero(detailEntity.getDamagedQuantity()),
                "shortageQuantity", valueOrZero(detailEntity.getShortageQuantity()),
                "excessQuantity", valueOrZero(detailEntity.getExcessQuantity())
        )).toList());
        return detail;
    }

    private PurchaseOrderDeliveryEntity findDeliveryForUpdate(String purchaseOrderNo, Long deliveryId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderDeliveryEntity delivery = findDelivery(purchaseOrderNo, deliveryId);
        return deliveryRepository.findAllByIdForUpdate(List.of(deliveryId)).stream()
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Purchase order delivery " + deliveryId + " not found."));
    }

    private PurchaseOrderDeliveryEntity findDelivery(String purchaseOrderNo, Long deliveryId)
            throws DataNotFoundException, InvalidRequestException {
        if (deliveryId == null) throw new InvalidRequestException("deliveryId is required.");
        purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderDeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DataNotFoundException("Purchase order delivery " + deliveryId + " not found."));
        if (!purchaseOrderNo.equals(delivery.getPurchaseOrder().getPurchaseOrderNo())) {
            throw new InvalidRequestException("Delivery does not belong to this purchase order.");
        }
        return delivery;
    }

    private void refreshDeliveryStatus(PurchaseOrderDeliveryEntity delivery) {
        List<PurchaseOrderReceiptDetailEntity> details = receiptRepository
                .findByDelivery_IdOrderByCreatedDateDesc(delivery.getId()).stream()
                .filter(receipt -> receipt.getStatus() == PurchaseOrderReceiptStatus.CONFIRMED)
                .flatMap(receipt -> receipt.getDetails().stream())
                .toList();
        Map<Long, Totals> totals = new HashMap<>();
        details.forEach(detail -> totals.merge(detail.getDeliveryDetail().getId(),
                new Totals(valueOrZero(detail.getReceivedQuantity()), valueOrZero(detail.getDamagedQuantity()),
                        valueOrZero(detail.getShortageQuantity()), valueOrZero(detail.getExcessQuantity())),
                Totals::add));
        boolean anyHandled = false;
        boolean allHandled = true;
        boolean issue = false;
        for (PurchaseOrderDeliveryDetailEntity detail : delivery.getDetails()) {
            Totals total = totals.getOrDefault(detail.getId(), Totals.ZERO);
            BigDecimal shipped = valueOrZero(detail.getQuantity());
            anyHandled |= total.handled().signum() > 0;
            allHandled &= total.handled().compareTo(shipped) >= 0;
            issue |= total.damaged.signum() > 0 || total.shortage.signum() > 0 || total.excess.signum() > 0;
        }
        if (allHandled && !delivery.getDetails().isEmpty()) {
            delivery.setStatus(issue ? PurchaseOrderDeliveryStatus.RECEIVED_WITH_ISSUE
                    : PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE);
            delivery.setReceivedAt(ZonedDateTime.now(DateUtil.getTimeZone()));
        } else if (anyHandled) {
            delivery.setStatus(PurchaseOrderDeliveryStatus.PARTIALLY_RECEIVED);
        } else {
            delivery.setStatus(PurchaseOrderDeliveryStatus.IN_TRANSIT);
            delivery.setReceivedAt(null);
        }
        deliveryRepository.save(delivery);
    }

    private List<PurchaseOrderReceiptDetailDto> buildContextDetails(PurchaseOrderDeliveryEntity delivery) {
        Map<Long, Totals> previous = previousTotals(delivery);
        return delivery.getDetails().stream().map(detail -> {
            BigDecimal shipped = valueOrZero(detail.getQuantity());
            Totals total = previous.getOrDefault(detail.getId(), Totals.ZERO);
            PurchaseOrderReceiptDetailDto dto = baseDetailDto(detail);
            dto.setShippedQuantity(shipped);
            dto.setReceivedQuantityToDate(total.received);
            dto.setRemainingQuantity(shipped.subtract(total.handled()).max(BigDecimal.ZERO));
            return dto;
        }).toList();
    }

    private Map<Long, Totals> previousTotals(PurchaseOrderDeliveryEntity delivery) {
        return receiptRepository.findByDelivery_IdOrderByCreatedDateDesc(delivery.getId()).stream()
                .filter(receipt -> receipt.getStatus() == PurchaseOrderReceiptStatus.CONFIRMED)
                .flatMap(receipt -> receipt.getDetails().stream())
                .collect(Collectors.toMap(
                        detail -> detail.getDeliveryDetail().getId(),
                        detail -> new Totals(valueOrZero(detail.getReceivedQuantity()), valueOrZero(detail.getDamagedQuantity()),
                                valueOrZero(detail.getShortageQuantity()), valueOrZero(detail.getExcessQuantity())),
                        Totals::add));
    }

    private PurchaseOrderReceiptDto baseDto(PurchaseOrderDeliveryEntity delivery) {
        PurchaseOrderReceiptDto dto = new PurchaseOrderReceiptDto();
        dto.setPurchaseOrderNo(delivery.getPurchaseOrder().getPurchaseOrderNo());
        dto.setDeliveryId(delivery.getId());
        dto.setDeliveryNo(delivery.getDeliveryNo());
        dto.setSupplierName(delivery.getPurchaseOrder().getSupplierNameSnapshot() != null
                ? delivery.getPurchaseOrder().getSupplierNameSnapshot()
                : delivery.getPurchaseOrder().getSupplier().getSupplierName());
        dto.setShippingMethod(delivery.getShippingMethod());
        dto.setTrackingNumber(delivery.getTrackingNumber());
        if (delivery.getSupplierShippingRoute() != null
                && delivery.getSupplierShippingRoute().getShippingRoute() != null) {
            var route = delivery.getSupplierShippingRoute().getShippingRoute();
            dto.setShippingRouteCode(route.getRouteCode());
            dto.setOriginLocationName(route.getOriginLocation() == null ? null : route.getOriginLocation().getName());
            dto.setDestinationLocationName(route.getDestinationLocation() == null ? null : route.getDestinationLocation().getName());
        }
        dto.setWarehouseId(null);
        return dto;
    }

    private PurchaseOrderReceiptDto mapReceipt(PurchaseOrderReceiptEntity receipt, PurchaseOrderDeliveryEntity delivery) {
        PurchaseOrderReceiptDto dto = baseDto(delivery);
        dto.setReceiptNo(receipt.getReceiptNo());
        dto.setWarehouseId(receipt.getWarehouseId());
        dto.setReceivedDate(receipt.getReceivedDate());
        dto.setReceivedBy(receipt.getReceivedBy() == null ? null : receipt.getReceivedBy().getId());
        dto.setStatus(receipt.getStatus());
        dto.setRemark(receipt.getRemark());
        dto.setCreatedDate(receipt.getCreatedDate());
        dto.setDetails(receipt.getDetails().stream().map(detail -> {
            PurchaseOrderReceiptDetailDto dtoDetail = baseDetailDto(detail.getDeliveryDetail());
            dtoDetail.setId(detail.getId());
            dtoDetail.setShippedQuantity(detail.getShippedQuantity());
            dtoDetail.setReceivedQuantity(detail.getReceivedQuantity());
            dtoDetail.setDamagedQuantity(detail.getDamagedQuantity());
            dtoDetail.setShortageQuantity(detail.getShortageQuantity());
            dtoDetail.setExcessQuantity(detail.getExcessQuantity());
            dtoDetail.setLotNo(detail.getLotNo());
            dtoDetail.setExpiryDate(detail.getExpiryDate());
            dtoDetail.setWarehouseLocationId(detail.getWarehouseLocationId());
            dtoDetail.setRemark(detail.getRemark());
            return dtoDetail;
        }).toList());
        return dto;
    }

    private PurchaseOrderReceiptDetailDto baseDetailDto(PurchaseOrderDeliveryDetailEntity detail) {
        PurchaseOrderReceiptDetailDto dto = new PurchaseOrderReceiptDetailDto();
        dto.setDeliveryDetailId(detail.getId());
        dto.setPurchaseOrderDetailId(detail.getPurchaseOrderDetail().getId());
        dto.setPurchaseOrderDetailComponentId(detail.getPurchaseOrderDetailComponent() == null
                ? null : detail.getPurchaseOrderDetailComponent().getId());
        dto.setProductName(detail.getProductNameSnapshot());
        dto.setComponentName(detail.getComponentNameSnapshot());
        return dto;
    }

    private BigDecimal nonNegative(BigDecimal value, String field) throws InvalidRequestException {
        BigDecimal result = valueOrZero(value);
        if (result.signum() < 0) throw new InvalidRequestException(field + " must not be negative.");
        return result;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record Totals(BigDecimal received, BigDecimal damaged, BigDecimal shortage, BigDecimal excess) {
        private static final Totals ZERO = new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        private Totals add(Totals other) {
            return new Totals(received.add(other.received), damaged.add(other.damaged),
                    shortage.add(other.shortage), excess.add(other.excess));
        }

        private BigDecimal handled() {
            return received.add(damaged).add(shortage);
        }
    }
}
