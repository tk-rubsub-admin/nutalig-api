package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.SalesOrderPaymentStatus;
import com.nutalig.constant.SalesOrderStatus;
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
@Table(name = "sales_order")
@ToString(onlyExplicitlyIncluded = true)
public class SalesOrderEntity extends AuditDateEntity {

    @Id
    @Column(name = "sales_order_no", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String salesOrderNo;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private SalesOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", referencedColumnName = "employee_id")
    @ToString.Exclude
    private EmployeeEntity sales;

    @Column(name = "co_sales_id", length = 20)
    private String coSalesId;

    @Column(name = "subtotal", precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount", precision = 18, scale = 2)
    private BigDecimal discount;

    @Column(name = "freight", precision = 18, scale = 2)
    private BigDecimal freight;

    @Column(name = "vat", precision = 18, scale = 2)
    private BigDecimal vat;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "shipping_type", length = 10)
    private String shippingType;

    @Column(name = "vat_rate", precision = 5, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "remark", length = 2000)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_address_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerAddressEntity customerAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_contact_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerContactEntity customerContact;

    @Column(name = "rev_no")
    private Integer revNo;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private SalesOrderPaymentStatus paymentStatus;

    @Column(name = "paid_total", precision = 18, scale = 2)
    private BigDecimal paidTotal;

    @Column(name = "outstanding_total", precision = 18, scale = 2)
    private BigDecimal outstandingTotal;

    @Column(name = "commission", precision = 18, scale = 2)
    private BigDecimal commission;

    @Column(name = "co_sale_commission", precision = 18, scale = 2)
    private BigDecimal coSaleCommission;

    @Column(name = "request_coa")
    private Boolean requestCoa;

    @Column(name = "request_po")
    private Boolean requestPo;

    @Enumerated(EnumType.STRING)
    @Column(name = "procurement_status", length = 30)
    private ProcurementStatus procurementStatus;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @ToString.Exclude
    private Set<SalesOrderAttachmentEntity> attachments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<SalesOrderDetailEntity> items = new LinkedHashSet<>();

    public void addAttachment(SalesOrderAttachmentEntity attachment) {
        if (attachment == null) return;
        attachments.add(attachment);
        attachment.setSalesOrder(this);
    }

    public void removeAttachment(SalesOrderAttachmentEntity attachment) {
        if (attachment == null) return;
        attachments.remove(attachment);
        attachment.setSalesOrder(null);
    }

    public void addItem(SalesOrderDetailEntity item) {
        if (item == null) return;
        items.add(item);
        item.setSalesOrder(this);
    }

    public void removeItem(SalesOrderDetailEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setSalesOrder(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalesOrderEntity that)) return false;
        return salesOrderNo != null && salesOrderNo.equals(that.salesOrderNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salesOrderNo);
    }
}
