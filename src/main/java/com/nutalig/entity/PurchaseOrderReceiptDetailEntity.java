package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_receipt_detail")
public class PurchaseOrderReceiptDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_no", referencedColumnName = "receipt_no", nullable = false)
    private PurchaseOrderReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_detail_id", nullable = false)
    private PurchaseOrderDeliveryDetailEntity deliveryDetail;


    @Column(name = "shipped_quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal shippedQuantity;

    @Column(name = "received_quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "damaged_quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal damagedQuantity = BigDecimal.ZERO;

    @Column(name = "shortage_quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal shortageQuantity = BigDecimal.ZERO;

    @Column(name = "excess_quantity", precision = 18, scale = 5, nullable = false)
    private BigDecimal excessQuantity = BigDecimal.ZERO;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "warehouse_location_id")
    private Long warehouseLocationId;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderReceiptDetailEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
