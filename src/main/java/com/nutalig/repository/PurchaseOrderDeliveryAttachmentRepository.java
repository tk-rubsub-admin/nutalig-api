package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderDeliveryAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderDeliveryAttachmentRepository
        extends JpaRepository<PurchaseOrderDeliveryAttachmentEntity, Long> {
    List<PurchaseOrderDeliveryAttachmentEntity> findByDeliveryIdAndActiveTrueOrderByUploadedAtAscIdAsc(Long deliveryId);

    Optional<PurchaseOrderDeliveryAttachmentEntity> findByIdAndDeliveryIdAndActiveTrue(
            Long id, Long deliveryId);
}
