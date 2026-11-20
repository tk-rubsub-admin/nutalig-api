package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderReceiptStockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderReceiptStockMovementRepository
        extends JpaRepository<PurchaseOrderReceiptStockMovementEntity, Long> {
    List<PurchaseOrderReceiptStockMovementEntity> findByReceipt_ReceiptNoAndMovementType(
            String receiptNo,
            com.nutalig.constant.PurchaseOrderReceiptStockMovementType movementType
    );

    boolean existsByReverseOf_Id(Long movementId);
}
