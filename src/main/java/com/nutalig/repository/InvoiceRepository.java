package com.nutalig.repository;

import com.nutalig.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String>, JpaSpecificationExecutor<InvoiceEntity> {
    List<InvoiceEntity> findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(String salesOrderNo);
}
