package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_detail_component")
public class PurchaseOrderDetailComponentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_detail_id", nullable = false)
    private PurchaseOrderDetailEntity purchaseOrderDetail;

    @Column(name = "source_quote_detail_package_id")
    private Long sourceQuoteDetailPackageId;

    @Column(name = "component_code", length = 100)
    private String componentCode;

    @Column(name = "component_name", length = 255, nullable = false)
    private String componentName;

    @Column(name = "specification", columnDefinition = "TEXT")
    private String specification;

    @Column(name = "quantity_per_item", precision = 18, scale = 5, nullable = false)
    private BigDecimal quantityPerItem;

    @Column(name = "unit", length = 50, nullable = false)
    private String unit;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
