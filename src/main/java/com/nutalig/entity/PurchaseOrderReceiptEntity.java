package com.nutalig.entity;

import com.nutalig.constant.PurchaseOrderReceiptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_receipt")
public class PurchaseOrderReceiptEntity extends AuditDateEntity {
    @Id
    @Column(name = "receipt_no", nullable = false, unique = true, length = 50)
    private String receiptNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_no", referencedColumnName = "purchase_order_no", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private PurchaseOrderDeliveryEntity delivery;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by", referencedColumnName = "id", nullable = false)
    private UserEntity receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderReceiptStatus status = PurchaseOrderReceiptStatus.CONFIRMED;

    @Column(name = "remark", length = 1000)
    private String remark;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<PurchaseOrderReceiptDetailEntity> details = new ArrayList<>();

    public void addDetail(PurchaseOrderReceiptDetailEntity detail) {
        if (detail == null) return;
        details.add(detail);
        detail.setReceipt(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderReceiptEntity that)) return false;
        return receiptNo != null && receiptNo.equals(that.receiptNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptNo);
    }
}
