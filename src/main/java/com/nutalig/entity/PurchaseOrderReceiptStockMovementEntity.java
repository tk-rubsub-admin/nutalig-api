package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderReceiptStockMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_receipt_stock_movement")
public class PurchaseOrderReceiptStockMovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_no", referencedColumnName = "receipt_no", nullable = false)
    private PurchaseOrderReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_detail_id", nullable = false)
    private PurchaseOrderReceiptDetailEntity receiptDetail;

    @Column(name = "delivery_detail_id", nullable = false)
    private Long deliveryDetailId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_location_id")
    private Long warehouseLocationId;

    @Column(name = "purchase_order_detail_id", nullable = false)
    private Long purchaseOrderDetailId;

    @Column(name = "purchase_order_detail_component_id")
    private Long purchaseOrderDetailComponentId;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private PurchaseOrderReceiptStockMovementType movementType;

    @Column(name = "quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal quantity;

    @Column(name = "movement_at", nullable = false)
    private ZonedDateTime movementAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverse_of_id")
    private PurchaseOrderReceiptStockMovementEntity reverseOf;
}
