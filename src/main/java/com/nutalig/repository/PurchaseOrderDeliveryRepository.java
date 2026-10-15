package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderDeliveryRepository extends JpaRepository<PurchaseOrderDeliveryEntity, Long> {
    long countByPurchaseOrder_PurchaseOrderNo(String purchaseOrderNo);

    List<PurchaseOrderDeliveryEntity> findAllByPurchaseOrder_PurchaseOrderNo(String purchaseOrderNo);
}
