package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_delivery_detail")
public class PurchaseOrderDeliveryDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_delivery_id", nullable = false)
    private PurchaseOrderDeliveryEntity purchaseOrderDelivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_detail_id", nullable = false)
    private PurchaseOrderDetailEntity purchaseOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_detail_component_id")
    private PurchaseOrderDetailComponentEntity purchaseOrderDetailComponent;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "component_name_snapshot", length = 255)
    private String componentNameSnapshot;

    @Column(name = "quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal quantity;

    @Column(name = "carton_count", precision = 18, scale = 5)
    private BigDecimal cartonCount;

    @Column(name = "pieces_per_carton", precision = 18, scale = 5)
    private BigDecimal piecesPerCarton;

    @Column(name = "net_weight_kg", precision = 18, scale = 4)
    private BigDecimal netWeightKg;

    @Column(name = "gross_weight_kg", precision = 18, scale = 4)
    private BigDecimal grossWeightKg;

    @Column(name = "length_cm", precision = 18, scale = 4)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 18, scale = 4)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 18, scale = 4)
    private BigDecimal heightCm;

    @Column(name = "cbm_m3", precision = 18, scale = 6)
    private BigDecimal cbmM3;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderDeliveryDetailEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
