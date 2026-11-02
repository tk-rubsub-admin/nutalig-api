package com.nutalig.repository;

import com.nutalig.entity.SalesOrderPaymentScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderPaymentScheduleRepository extends JpaRepository<SalesOrderPaymentScheduleEntity, Long> {
    List<SalesOrderPaymentScheduleEntity> findBySalesOrderSalesOrderNoOrderByInstallmentNoAsc(String salesOrderNo);
}
