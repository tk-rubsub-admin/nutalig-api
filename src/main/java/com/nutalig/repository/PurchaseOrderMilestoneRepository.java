package com.nutalig.repository;

import com.nutalig.constant.PurchaseOrderMilestoneCode;
import com.nutalig.entity.PurchaseOrderMilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderMilestoneRepository extends JpaRepository<PurchaseOrderMilestoneEntity, Long> {

    List<PurchaseOrderMilestoneEntity> findAllByPurchaseOrder_PurchaseOrderNo(String purchaseOrderNo);

    Optional<PurchaseOrderMilestoneEntity> findByPurchaseOrder_PurchaseOrderNoAndMilestoneCode(
            String purchaseOrderNo,
            PurchaseOrderMilestoneCode milestoneCode
    );
}
