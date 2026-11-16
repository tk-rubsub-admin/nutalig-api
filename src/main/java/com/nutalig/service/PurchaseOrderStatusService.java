package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.entity.PurchaseOrderDeliveryDetailEntity;
import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import com.nutalig.entity.PurchaseOrderDetailComponentEntity;
import com.nutalig.entity.PurchaseOrderDetailEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseOrderStatusService {

    private static final Set<PurchaseOrderDeliveryStatus> SHIPPED_STATUSES = Set.of(
            PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER,
            PurchaseOrderDeliveryStatus.IN_TRANSIT,
            PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE
    );

    private final PurchaseOrderDeliveryRepository purchaseOrderDeliveryRepository;

    @Transactional
    public void refresh(PurchaseOrderEntity purchaseOrder) {
        if (purchaseOrder == null
                || purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED
                || purchaseOrder.getStatus() == PurchaseOrderStatus.CLOSED) {
            return;
        }

        List<RequiredQuantity> requiredQuantities = requiredQuantities(purchaseOrder);
        if (requiredQuantities.isEmpty()) return;

        Map<DeliveryKey, BigDecimal> produced = new HashMap<>();
        Map<DeliveryKey, BigDecimal> shipped = new HashMap<>();
        Map<DeliveryKey, BigDecimal> received = new HashMap<>();
        List<PurchaseOrderDeliveryEntity> deliveries = purchaseOrderDeliveryRepository
                .findAllByPurchaseOrder_PurchaseOrderNo(purchaseOrder.getPurchaseOrderNo());

        for (PurchaseOrderDeliveryEntity delivery : deliveries) {
            if (delivery.getStatus() == PurchaseOrderDeliveryStatus.CANCELLED) continue;
            for (PurchaseOrderDeliveryDetailEntity detail : delivery.getDetails()) {
                DeliveryKey key = new DeliveryKey(
                        detail.getPurchaseOrderDetail().getId(),
                        detail.getPurchaseOrderDetailComponent() == null
                                ? null : detail.getPurchaseOrderDetailComponent().getId());
                BigDecimal quantity = valueOrZero(detail.getQuantity());
                add(produced, key, quantity);
                if (SHIPPED_STATUSES.contains(delivery.getStatus())) add(shipped, key, quantity);
                if (delivery.getStatus() == PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE) {
                    add(received, key, quantity);
                }
            }
        }

        boolean hasProduced = hasPositive(produced);
        boolean hasShipped = hasPositive(shipped);
        boolean hasReceived = hasPositive(received);
        boolean allProduced = meetsRequiredQuantities(requiredQuantities, produced);
        boolean allShipped = meetsRequiredQuantities(requiredQuantities, shipped);
        boolean allReceived = meetsRequiredQuantities(requiredQuantities, received);

        PurchaseOrderStatus nextStatus;
        if (hasReceived && allReceived) {
            nextStatus = PurchaseOrderStatus.RECEIVED;
        } else if (hasReceived) {
            nextStatus = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        } else if (hasShipped && allShipped) {
            nextStatus = PurchaseOrderStatus.IN_TRANSIT;
        } else if (hasShipped) {
            nextStatus = PurchaseOrderStatus.PARTIALLY_IN_TRANSIT;
        } else if (allProduced) {
            nextStatus = PurchaseOrderStatus.PRODUCTION_COMPLETED;
        } else if (hasProduced) {
            nextStatus = PurchaseOrderStatus.PRODUCTION_PARTIALLY_COMPLETED;
        } else {
            return;
        }

        if (allProduced && purchaseOrder.getProductionCompletedDate() == null) {
            purchaseOrder.setProductionCompletedDate(LocalDate.now(DateUtil.getTimeZone()));
        }
        if (purchaseOrder.getStatus() != nextStatus) {
            purchaseOrder.setStatus(nextStatus);
            purchaseOrder.setUpdatedDate(java.time.ZonedDateTime.now(DateUtil.getTimeZone()));
        }
    }

    private List<RequiredQuantity> requiredQuantities(PurchaseOrderEntity purchaseOrder) {
        List<RequiredQuantity> result = new ArrayList<>();
        for (PurchaseOrderDetailEntity item : purchaseOrder.getItems()) {
            if (item.getLineType() != null
                    && item.getLineType() != com.nutalig.constant.OrderLineType.PRODUCT) continue;
            if (item.getComponents() == null || item.getComponents().isEmpty()) {
                addRequired(result, item.getId(), null, item.getQuantity());
                continue;
            }
            for (PurchaseOrderDetailComponentEntity component : item.getComponents()) {
                BigDecimal ordered = valueOrZero(item.getQuantity())
                        .multiply(valueOrZero(component.getQuantityPerItem()));
                addRequired(result, item.getId(), component.getId(), ordered);
            }
        }
        return result;
    }

    private void addRequired(List<RequiredQuantity> result, Long detailId, Long componentId, BigDecimal quantity) {
        if (quantity != null && quantity.signum() > 0) {
            result.add(new RequiredQuantity(new DeliveryKey(detailId, componentId), quantity));
        }
    }

    private boolean meetsRequiredQuantities(List<RequiredQuantity> required, Map<DeliveryKey, BigDecimal> actual) {
        return required.stream().allMatch(row -> actual.getOrDefault(row.key(), BigDecimal.ZERO)
                .compareTo(row.quantity()) >= 0);
    }

    private boolean hasPositive(Map<DeliveryKey, BigDecimal> values) {
        return values.values().stream().anyMatch(value -> value.signum() > 0);
    }

    private void add(Map<DeliveryKey, BigDecimal> values, DeliveryKey key, BigDecimal quantity) {
        values.merge(key, quantity, BigDecimal::add);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record DeliveryKey(Long purchaseOrderDetailId, Long componentId) {
    }

    private record RequiredQuantity(DeliveryKey key, BigDecimal quantity) {
    }
}
