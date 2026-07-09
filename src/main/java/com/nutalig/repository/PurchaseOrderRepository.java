package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, String>, JpaSpecificationExecutor<PurchaseOrderEntity> {
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoAndSupplierIdOrderByCreatedDateDesc(String salesOrderNo, String supplierId);
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoAndSupplierShippingIdOrderByCreatedDateDesc(String salesOrderNo, Long supplierShippingId);
    List<PurchaseOrderEntity> findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(String salesOrderNo);
}
