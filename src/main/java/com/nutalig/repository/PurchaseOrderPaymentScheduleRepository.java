package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderPaymentScheduleRepository extends JpaRepository<PurchaseOrderPaymentScheduleEntity, Long>,
        JpaSpecificationExecutor<PurchaseOrderPaymentScheduleEntity> {
    List<PurchaseOrderPaymentScheduleEntity> findByPurchaseOrderPurchaseOrderNoOrderByInstallmentNoAsc(String purchaseOrderNo);
}
