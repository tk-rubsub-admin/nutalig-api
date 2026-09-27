package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderProofRevisionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderProofRevisionRepository extends JpaRepository<PurchaseOrderProofRevisionEntity, Long> {

    @EntityGraph(attributePaths = {"proof", "proof.purchaseOrder", "proof.assignedSalesUser", "attachments"})
    Optional<PurchaseOrderProofRevisionEntity> findFirstByProof_IdOrderByRevisionNoDesc(Long proofId);
}
