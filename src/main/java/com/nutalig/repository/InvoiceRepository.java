package com.nutalig.repository;

import com.nutalig.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.repository.query.Param;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String>, JpaSpecificationExecutor<InvoiceEntity> {
    List<InvoiceEntity> findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(String salesOrderNo);

    @Query("""
            select coalesce(sum(payment.amount), 0)
            from InvoiceEntity invoice
            join invoice.payments payment
            where invoice.salesOrder.salesOrderNo = :salesOrderNo
              and payment.status = com.nutalig.constant.InvoicePaymentStatus.APPROVE
            """)
    BigDecimal sumApprovedPaymentAmountBySalesOrderNo(@Param("salesOrderNo") String salesOrderNo);
}
