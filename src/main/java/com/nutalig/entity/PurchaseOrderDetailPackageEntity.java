package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_detail_package")
public class PurchaseOrderDetailPackageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_detail_id", nullable = false)
    private PurchaseOrderDetailEntity purchaseOrderDetail;

    @Column(name = "source_package_id") private Long sourcePackageId;
    @Column(name = "package_name") private String packageName;
    @Column(name = "package_dimension") private String packageDimension;
    @Column(name = "package_weight") private String packageWeight;
    @Column(name = "package_capacity") private String packageCapacity;
    @Column(name = "width_cm", precision = 18, scale = 4) private BigDecimal widthCm;
    @Column(name = "length_cm", precision = 18, scale = 4) private BigDecimal lengthCm;
    @Column(name = "height_cm", precision = 18, scale = 4) private BigDecimal heightCm;
    @Column(name = "capacity_qty", precision = 18, scale = 4) private BigDecimal capacityQty;
    @Column(name = "carton_count") private Long cartonCount;
    @Column(name = "cbm_per_carton", precision = 18, scale = 6) private BigDecimal cbmPerCarton;
    @Column(name = "total_cbm", precision = 18, scale = 6) private BigDecimal totalCbm;
    @Column(name = "selected_for_calculation", nullable = false) private Boolean selectedForCalculation = false;
    @Column(name = "sort_order") private Integer sortOrder;
}
