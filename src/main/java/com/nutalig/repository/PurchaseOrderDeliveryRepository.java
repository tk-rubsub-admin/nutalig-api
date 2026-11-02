package com.nutalig.repository;

import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseOrderDeliveryRepository extends JpaRepository<PurchaseOrderDeliveryEntity, Long>, JpaSpecificationExecutor<PurchaseOrderDeliveryEntity> {
    @Override
    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier", "purchaseOrder.salesOrder", "shippingLocation"})
    Page<PurchaseOrderDeliveryEntity> findAll(Specification<PurchaseOrderDeliveryEntity> specification, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from PurchaseOrderDeliveryEntity d where d.id in :ids order by d.id")
    List<PurchaseOrderDeliveryEntity> findAllByIdForUpdate(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    long countByPurchaseOrder_PurchaseOrderNo(String purchaseOrderNo);

    List<PurchaseOrderDeliveryEntity> findAllByPurchaseOrder_PurchaseOrderNo(String purchaseOrderNo);
}
