package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "purchase_order")
@ToString(onlyExplicitlyIncluded = true)
public class PurchaseOrderEntity extends AuditDateEntity {

    @Id
    @Column(name = "purchase_order_no", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String purchaseOrderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no", nullable = false)
    @ToString.Exclude
    private SalesOrderEntity salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private SupplierEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_shipping_id", referencedColumnName = "id")
    @ToString.Exclude
    private SupplierShippingEntity supplierShipping;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(name = "production_lead_time_day")
    private Integer productionLeadTimeDay;

    @Column(name = "shipping_lead_time_day")
    private Integer shippingLeadTimeDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PurchaseOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @Column(name = "sub_total", precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "sub_total_thb", precision = 18, scale = 2)
    private BigDecimal subTotalThb;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "grand_total_thb", precision = 18, scale = 2)
    private BigDecimal grandTotalThb;

    @Column(name = "remark", length = 2000)
    private String remark;

    @Column(name = "rev_no")
    private Integer revNo;

    @Column(name = "supplier_name_snapshot", length = 255)
    private String supplierNameSnapshot;

    @Column(name = "supplier_address_snapshot", length = 2000)
    private String supplierAddressSnapshot;

    @Column(name = "supplier_contact_snapshot", length = 255)
    private String supplierContactSnapshot;

    @Column(name = "supplier_phone_snapshot", length = 100)
    private String supplierPhoneSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<PurchaseOrderDetailEntity> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @ToString.Exclude
    private Set<PurchaseOrderAttachmentEntity> attachments = new LinkedHashSet<>();

    public void addItem(PurchaseOrderDetailEntity item) {
        if (item == null) return;
        items.add(item);
        item.setPurchaseOrder(this);
    }

    public void removeItem(PurchaseOrderDetailEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setPurchaseOrder(null);
    }

    public void addAttachment(PurchaseOrderAttachmentEntity attachment) {
        if (attachment == null) return;
        attachments.add(attachment);
        attachment.setPurchaseOrder(this);
    }

    public void removeAttachment(PurchaseOrderAttachmentEntity attachment) {
        if (attachment == null) return;
        attachments.remove(attachment);
        attachment.setPurchaseOrder(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderEntity that)) return false;
        return purchaseOrderNo != null && purchaseOrderNo.equals(that.purchaseOrderNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purchaseOrderNo);
    }
}
