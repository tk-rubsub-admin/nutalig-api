package com.nutalig.repository;

import com.nutalig.entity.SalesPerformanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalesPerformanceTransactionRepository extends JpaRepository<SalesPerformanceTransactionEntity, Long> {

    void deleteBySalesOrderNo(String salesOrderNo);

    interface MonthlySummary {
        String getSalesId();
        Integer getSalesYear();
        Integer getSalesMonth();
        String getCurrency();
        java.math.BigDecimal getDepositAmount();
        java.math.BigDecimal getSalesAmount();
        java.math.BigDecimal getInterimInstallmentAmount();
    }

    @Query(value = """
        SELECT sales_id AS salesId, YEAR(payment_date) AS salesYear, MONTH(payment_date) AS salesMonth,
               currency AS currency,
               SUM(CASE WHEN performance_type = 'DEPOSIT' THEN amount ELSE 0 END) AS depositAmount,
               SUM(CASE WHEN performance_type = 'SALE' THEN amount ELSE 0 END) AS salesAmount,
               SUM(CASE WHEN performance_type = 'INTERIM_INSTALLMENT' THEN amount ELSE 0 END) AS interimInstallmentAmount
        FROM sales_performance_transaction
        WHERE payment_date >= :dateFrom AND payment_date <= :dateTo
          AND (:salesId IS NULL OR sales_id = :salesId)
        GROUP BY sales_id, YEAR(payment_date), MONTH(payment_date), currency
        ORDER BY YEAR(payment_date), MONTH(payment_date), sales_id, currency
        """, nativeQuery = true)
    List<MonthlySummary> findMonthlySummary(@Param("dateFrom") LocalDate dateFrom,
                                           @Param("dateTo") LocalDate dateTo,
                                           @Param("salesId") String salesId);
}
