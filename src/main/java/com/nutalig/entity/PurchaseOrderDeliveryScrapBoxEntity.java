package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_delivery_scrap_box")
public class PurchaseOrderDeliveryScrapBoxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_delivery_id", nullable = false)
    private PurchaseOrderDeliveryEntity purchaseOrderDelivery;

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
}
