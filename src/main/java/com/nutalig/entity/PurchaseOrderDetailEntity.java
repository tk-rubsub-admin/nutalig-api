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
@Table(name = "purchase_order_detail")
@ToString(onlyExplicitlyIncluded = true)
public class PurchaseOrderDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    @ToString.Exclude
    private PurchaseOrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_detail_id", referencedColumnName = "id")
    @ToString.Exclude
    private SalesOrderDetailEntity salesOrderDetail;

    @Column(name = "line_no")
    private Integer lineNo;

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

    @Column(name = "quantity", precision = 18, scale = 5)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_currency", length = 10)
    private Currency supplierCurrency;

    @Column(name = "supplier_unit_price", precision = 18, scale = 4)
    private BigDecimal supplierUnitPrice;

    @Column(name = "supplier_shipping_cost", precision = 18, scale = 4)
    private BigDecimal supplierShippingCost;

    @Column(name = "supplier_total_unit_cost", precision = 18, scale = 4)
    private BigDecimal supplierTotalUnitCost;

    @Column(name = "amount_supplier_currency", precision = 18, scale = 5)
    private BigDecimal amountSupplierCurrency;

    @Column(name = "amount_thb", precision = 18, scale = 5)
    private BigDecimal amountThb;

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

    @Column(name = "supplier_quote_tier_id")
    private Long supplierQuoteTierId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderDetailEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? Objects.hash(id) : System.identityHashCode(this);
    }
}
