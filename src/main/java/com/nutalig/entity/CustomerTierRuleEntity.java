package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity(name = "CustomerTierRule")
@Table(name = "customer_tier_rule")
public class CustomerTierRuleEntity extends AuditDateEntity {

    @Id
    @Column(name = "tier_code", length = 100, nullable = false)
    private String tierCode;

    @Column(name = "minimum_order_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_order_amount", precision = 18, scale = 2)
    private BigDecimal maximumOrderAmount;
}
