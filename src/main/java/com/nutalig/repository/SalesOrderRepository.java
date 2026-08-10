package com.nutalig.repository;

import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.entity.SalesOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrderEntity, String>, JpaSpecificationExecutor<SalesOrderEntity> {

    @Query("""
        select coalesce(sum(so.grandTotal), 0)
        from SalesOrderEntity so
        where so.customer.id = :customerId
          and so.status not in :excludedStatuses
    """)
    BigDecimal sumGrandTotalByCustomerIdAndStatusNotIn(
            @Param("customerId") String customerId,
            @Param("excludedStatuses") Collection<SalesOrderStatus> excludedStatuses
    );

    @Query("""
        select case when count(i) > 0 then true else false end
        from InvoiceEntity inv
        join inv.items i
        where i.salesOrderDetail.id = :salesOrderDetailId
    """)
    boolean existsInvoiceDetailBySalesOrderDetailId(
            @Param("salesOrderDetailId") Long salesOrderDetailId
    );

    @Query("""
        select case when count(i) > 0 then true else false end
        from PurchaseOrderEntity po
        join po.items i
        where i.salesOrderDetail.id = :salesOrderDetailId
    """)
    boolean existsPurchaseOrderDetailBySalesOrderDetailId(
            @Param("salesOrderDetailId") Long salesOrderDetailId
    );
}
