package com.nutalig.entity;

import com.nutalig.constant.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "sales_order_detail")
@ToString(onlyExplicitlyIncluded = true)
public class SalesOrderDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no", nullable = false)
    @ToString.Exclude
    private SalesOrderEntity salesOrder;

    @Column(name = "line_no")
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private SupplierEntity supplier;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "capacity", length = 255)
    private String capacity;

    @Column(name = "size", length = 255)
    private String size;

    @Column(name = "spec", length = 2000)
    private String spec;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", precision = 18, scale = 2)
    private BigDecimal quantity;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "rfq_detail_id")
    private Long rfqDetailId;

    @Column(name = "rfq_tier_id")
    private Long rfqTierId;

    @Column(name = "quotation_detail_id")
    private Long quotationDetailId;

    @Column(name = "shipping_method", length = 10)
    private String shippingMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_currency", length = 10)
    private Currency supplierCurrency;

    @Column(name = "supplier_unit_price", precision = 18, scale = 4)
    private BigDecimal supplierUnitPrice;

    @Column(name = "supplier_shipping_cost", precision = 18, scale = 4)
    private BigDecimal supplierShippingCost;

    @Column(name = "supplier_total_unit_cost", precision = 18, scale = 4)
    private BigDecimal supplierTotalUnitCost;

    @Column(name = "supplier_quote_tier_id")
    private Long supplierQuoteTierId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalesOrderDetailEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? Objects.hash(id) : System.identityHashCode(this);
    }
}
