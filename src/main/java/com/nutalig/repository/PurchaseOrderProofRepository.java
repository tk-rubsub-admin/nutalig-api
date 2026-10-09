package com.nutalig.repository;

import com.nutalig.constant.PurchaseOrderProofStatus;
import com.nutalig.entity.PurchaseOrderProofEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderProofRepository extends JpaRepository<PurchaseOrderProofEntity, Long> {

    @EntityGraph(attributePaths = {
            "purchaseOrder", "purchaseOrder.salesOrder", "purchaseOrder.salesOrder.customer",
            "assignedSalesUser", "proofType"
    })
    Optional<PurchaseOrderProofEntity> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select proof from PurchaseOrderProofEntity proof where proof.id = :id")
    Optional<PurchaseOrderProofEntity> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "assignedSalesUser", "proofType"
    })
    List<PurchaseOrderProofEntity> findAllByPurchaseOrder_PurchaseOrderNoOrderByIdAsc(String purchaseOrderNo);

    Optional<PurchaseOrderProofEntity> findByPurchaseOrder_PurchaseOrderNoAndProofType_Id_Code(
            String purchaseOrderNo,
            String proofTypeCode
    );

    boolean existsByPurchaseOrder_PurchaseOrderNoAndRequiredTrueAndStatusNot(
            String purchaseOrderNo,
            PurchaseOrderProofStatus status
    );
}
