package com.nutalig.entity;

import com.nutalig.constant.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "CustomerAnnualSalesOrder")
@Table(name = "customer_annual_sales_order")
public class CustomerAnnualSalesOrderEntity {

    @Id
    @Column(name = "sales_order_no", length = 50, nullable = false)
    private String salesOrderNo;

    @Column(name = "customer_id", length = 255, nullable = false)
    private String customerId;

    @Column(name = "sales_year", nullable = false)
    private Integer salesYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private Currency currency;

    @Column(name = "paid_in_full_date", nullable = false)
    private LocalDate paidInFullDate;

    @Column(name = "total_order_amount", precision = 18, scale = 5, nullable = false)
    private BigDecimal totalOrderAmount;
}
