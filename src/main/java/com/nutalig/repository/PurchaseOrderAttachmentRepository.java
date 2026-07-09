package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderAttachmentRepository extends JpaRepository<PurchaseOrderAttachmentEntity, Long> {
    List<PurchaseOrderAttachmentEntity> findByPurchaseOrderPurchaseOrderNoAndActiveTrueOrderBySortOrderAscIdAsc(String purchaseOrderNo);
    Optional<PurchaseOrderAttachmentEntity> findByIdAndPurchaseOrderPurchaseOrderNoAndActiveTrue(Long id, String purchaseOrderNo);
}
