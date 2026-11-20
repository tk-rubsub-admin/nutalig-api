package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderReceiptStockMovementType;
import com.nutalig.entity.PurchaseOrderReceiptDetailEntity;
import com.nutalig.entity.PurchaseOrderReceiptEntity;
import com.nutalig.entity.PurchaseOrderReceiptStockMovementEntity;
import com.nutalig.repository.PurchaseOrderReceiptStockMovementRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class PurchaseOrderReceiptStockMovementService {
    private final PurchaseOrderReceiptStockMovementRepository movementRepository;

    @Transactional
    public void recordReceipt(PurchaseOrderReceiptEntity receipt, String userId) {
        for (PurchaseOrderReceiptDetailEntity detail : receipt.getDetails()) {
            BigDecimal received = valueOrZero(detail.getReceivedQuantity());
            if (received.signum() <= 0) continue;
            PurchaseOrderReceiptStockMovementEntity movement = newMovement(
                    receipt, detail, received, PurchaseOrderReceiptStockMovementType.RECEIPT, userId);
            movementRepository.save(movement);
        }
    }

    @Transactional
    public void reverseReceipt(PurchaseOrderReceiptEntity receipt, String userId) {
        movementRepository.findByReceipt_ReceiptNoAndMovementType(
                        receipt.getReceiptNo(), PurchaseOrderReceiptStockMovementType.RECEIPT)
                .stream()
                .filter(movement -> !movementRepository.existsByReverseOf_Id(movement.getId()))
                .forEach(movement -> {
                    PurchaseOrderReceiptStockMovementEntity reverse = new PurchaseOrderReceiptStockMovementEntity();
                    reverse.setReceipt(receipt);
                    reverse.setReceiptDetail(movement.getReceiptDetail());
                    reverse.setDeliveryDetailId(movement.getDeliveryDetailId());
                    reverse.setWarehouseId(movement.getWarehouseId());
                    reverse.setWarehouseLocationId(movement.getWarehouseLocationId());
                    reverse.setPurchaseOrderDetailId(movement.getPurchaseOrderDetailId());
                    reverse.setPurchaseOrderDetailComponentId(movement.getPurchaseOrderDetailComponentId());
                    reverse.setItemName(movement.getItemName());
                    reverse.setMovementType(PurchaseOrderReceiptStockMovementType.REVERSE_RECEIPT);
                    reverse.setQuantity(movement.getQuantity().negate());
                    reverse.setMovementAt(ZonedDateTime.now(DateUtil.getTimeZone()));
                    reverse.setCreatedBy(userId);
                    reverse.setReverseOf(movement);
                    movementRepository.save(reverse);
                });
    }

    private PurchaseOrderReceiptStockMovementEntity newMovement(
            PurchaseOrderReceiptEntity receipt,
            PurchaseOrderReceiptDetailEntity detail,
            BigDecimal quantity,
            PurchaseOrderReceiptStockMovementType type,
            String userId
    ) {
        var deliveryDetail = detail.getDeliveryDetail();
        var movement = new PurchaseOrderReceiptStockMovementEntity();
        movement.setReceipt(receipt);
        movement.setReceiptDetail(detail);
        movement.setDeliveryDetailId(deliveryDetail.getId());
        movement.setWarehouseId(receipt.getWarehouseId());
        movement.setWarehouseLocationId(detail.getWarehouseLocationId());
        movement.setPurchaseOrderDetailId(deliveryDetail.getPurchaseOrderDetail().getId());
        movement.setPurchaseOrderDetailComponentId(deliveryDetail.getPurchaseOrderDetailComponent() == null
                ? null : deliveryDetail.getPurchaseOrderDetailComponent().getId());
        movement.setItemName(detail.getDeliveryDetail().getProductNameSnapshot());
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setMovementAt(ZonedDateTime.now(DateUtil.getTimeZone()));
        movement.setCreatedBy(userId);
        return movement;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
