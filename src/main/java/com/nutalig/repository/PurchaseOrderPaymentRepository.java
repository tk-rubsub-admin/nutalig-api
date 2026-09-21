package com.nutalig.repository;

import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface PurchaseOrderPaymentRepository extends JpaRepository<PurchaseOrderPaymentEntity, Long> {
    Optional<PurchaseOrderPaymentEntity> findByIdAndPurchaseOrderPurchaseOrderNo(Long id, String purchaseOrderNo);

    Optional<PurchaseOrderPaymentEntity> findByRequestKey(String requestKey);

    boolean existsByPurchaseOrderPurchaseOrderNoAndStatusIn(
            String purchaseOrderNo,
            Collection<PurchaseOrderPaymentStatus> statuses
    );
}
