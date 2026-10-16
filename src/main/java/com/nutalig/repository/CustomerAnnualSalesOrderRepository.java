package com.nutalig.repository;

import com.nutalig.constant.Currency;
import com.nutalig.entity.CustomerAnnualSalesOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CustomerAnnualSalesOrderRepository extends JpaRepository<CustomerAnnualSalesOrderEntity, String> {

    @Query("""
        select coalesce(sum(s.totalOrderAmount), 0)
        from CustomerAnnualSalesOrder s
        where s.customerId = :customerId and s.salesYear = :salesYear and s.currency = :currency
    """)
    BigDecimal sumAnnualAmount(@Param("customerId") String customerId,
                               @Param("salesYear") Integer salesYear,
                               @Param("currency") Currency currency);

    @Query("""
        select count(s)
        from CustomerAnnualSalesOrder s
        where s.customerId = :customerId and s.salesYear = :salesYear and s.currency = :currency
    """)
    long countAnnualOrders(@Param("customerId") String customerId,
                           @Param("salesYear") Integer salesYear,
                           @Param("currency") Currency currency);
}
