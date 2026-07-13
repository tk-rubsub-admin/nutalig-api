package com.nutalig.entity;

import com.nutalig.constant.Currency;
import jakarta.persistence.*;
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

    @Column(name = "shipping_cost", precision = 18, scale = 4)
    private BigDecimal shippingCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_price_currency", length = 10)
    private Currency productPriceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_cost_currency", length = 10)
    private Currency shippingCostCurrency;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;
}
