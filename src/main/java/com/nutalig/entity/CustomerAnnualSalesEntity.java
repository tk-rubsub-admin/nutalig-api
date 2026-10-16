package com.nutalig.entity;

import com.nutalig.entity.id.CustomerAnnualSalesId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity(name = "CustomerAnnualSales")
@Table(name = "customer_annual_sales")
public class CustomerAnnualSalesEntity extends AuditDateEntity {

    @EmbeddedId
    private CustomerAnnualSalesId id;

    @Column(name = "total_order_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalOrderAmount;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;
}
