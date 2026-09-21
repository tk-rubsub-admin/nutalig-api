package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, String>, JpaSpecificationExecutor<PurchaseOrderEntity> {
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoAndSupplierIdOrderByCreatedDateDesc(String salesOrderNo, String supplierId);
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoAndSupplierShippingIdOrderByCreatedDateDesc(String salesOrderNo, Long supplierShippingId);
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(String salesOrderNo);
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoIn(Collection<String> salesOrderNos);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PurchaseOrderEntity po where po.purchaseOrderNo = :purchaseOrderNo")
    Optional<PurchaseOrderEntity> findByIdForUpdate(@Param("purchaseOrderNo") String purchaseOrderNo);
}
