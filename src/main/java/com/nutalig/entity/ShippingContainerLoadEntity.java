package com.nutalig.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @Entity @Table(name = "shipping_container_load")
public class ShippingContainerLoadEntity extends AuditDateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "container_id", nullable = false)
    private ShippingContainerEntity container;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_id", nullable = false)
    private PurchaseOrderDeliveryEntity delivery;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delivery_detail_id")
    private PurchaseOrderDeliveryDetailEntity deliveryDetail;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scrap_box_id")
    private PurchaseOrderDeliveryScrapBoxEntity scrapBox;
    @Column(name = "active", nullable = false) private Boolean active = true;
    @Column(name = "active_scrap_box_id", unique = true) private Long activeScrapBoxId;
    @Column(name = "uniform_packaging_confirmed", nullable = false) private Boolean uniformPackagingConfirmed = false;
    @Column(name = "delivery_no_snapshot", length = 50)
    private String deliveryNo;
    @Column(name = "purchase_order_no_snapshot", length = 50)
    private String purchaseOrderNo;
    @Column(name = "component_name_snapshot", length = 255)
    private String componentName;
    @Column(name = "remark", length = 1000)
    private String remark;
    @Column(name = "carton_count", precision = 18, scale = 6)
    private BigDecimal cartonCount;
    @Column(name = "quantity", precision = 18, scale = 5)
    private BigDecimal quantity;
    @Column(name = "net_weight_kg", precision = 18, scale = 6)
    private BigDecimal netWeightKg;
    @Column(name = "gross_weight_kg", precision = 18, scale = 6)
    private BigDecimal grossWeightKg;
    @Column(name = "length_cm", precision = 18, scale = 6)
    private BigDecimal lengthCm;
    @Column(name = "width_cm", precision = 18, scale = 6)
    private BigDecimal widthCm;
    @Column(name = "height_cm", precision = 18, scale = 6)
    private BigDecimal heightCm;
    @Column(name = "cbm_m3", precision = 18, scale = 6)
    private BigDecimal cbmM3;
    @Column(name = "contents_json", columnDefinition = "TEXT", nullable = false) private String contentsJson;
}
