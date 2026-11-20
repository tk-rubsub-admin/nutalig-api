package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderReceiptRepository extends JpaRepository<PurchaseOrderReceiptEntity, String> {
    List<PurchaseOrderReceiptEntity> findByDelivery_IdOrderByCreatedDateDesc(Long deliveryId);

    List<PurchaseOrderReceiptEntity> findByPurchaseOrder_PurchaseOrderNoOrderByCreatedDateDesc(String purchaseOrderNo);

    Optional<PurchaseOrderReceiptEntity> findByReceiptNoAndPurchaseOrder_PurchaseOrderNo(
            String receiptNo, String purchaseOrderNo);
}
