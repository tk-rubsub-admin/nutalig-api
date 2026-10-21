package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderPaymentInstructionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderPaymentInstructionRepository
        extends JpaRepository<PurchaseOrderPaymentInstructionEntity, Long> {

    List<PurchaseOrderPaymentInstructionEntity> findBySchedule_PurchaseOrder_PurchaseOrderNoOrderByCreatedDateDescIdDesc(
            String purchaseOrderNo
    );

    List<PurchaseOrderPaymentInstructionEntity> findBySchedule_IdIn(List<Long> scheduleIds);

    Optional<PurchaseOrderPaymentInstructionEntity> findByIdAndSchedule_PurchaseOrder_PurchaseOrderNo(
            Long id,
            String purchaseOrderNo
    );

    @EntityGraph(attributePaths = {
            "schedule",
            "schedule.purchaseOrder",
            "schedule.purchaseOrder.salesOrder",
            "schedule.purchaseOrder.supplier"
    })
    Optional<PurchaseOrderPaymentInstructionEntity> findWithNotificationDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select instruction from PurchaseOrderPaymentInstructionEntity instruction " +
            "where instruction.id = :id and instruction.schedule.purchaseOrder.purchaseOrderNo = :purchaseOrderNo")
    Optional<PurchaseOrderPaymentInstructionEntity> findByIdForUpdate(
            @Param("id") Long id,
            @Param("purchaseOrderNo") String purchaseOrderNo
    );
}
