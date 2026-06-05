package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "rfq_supplier_quote_tier")
@Entity(name = "RfqSupplierQuoteTier")
public class RfqSupplierQuoteTierEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_detail_id", referencedColumnName = "id", nullable = false)
    private RfqSupplierQuoteDetailEntity quoteDetail;

    @Column(name = "quantity", precision = 18, scale = 0, nullable = false)
    private BigDecimal quantity;

    @Column(name = "product_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal productPrice;

    @Column(name = "land_freight_cost", precision = 18, scale = 4)
    private BigDecimal landFreightCost;

    @Column(name = "sea_freight_cost", precision = 18, scale = 4)
    private BigDecimal seaFreightCost;

    @Column(name = "land_total_price", precision = 18, scale = 4)
    private BigDecimal landTotalPrice;

    @Column(name = "sea_total_price", precision = 18, scale = 4)
    private BigDecimal seaTotalPrice;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
