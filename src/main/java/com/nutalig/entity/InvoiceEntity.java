package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.InvoiceStatus;
import com.nutalig.constant.UrgentRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "invoice")
@ToString(onlyExplicitlyIncluded = true)
public class InvoiceEntity extends AuditDateEntity {

    @Id
    @Column(name = "invoice_no", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no", nullable = false)
    @ToString.Exclude
    private SalesOrderEntity salesOrder;

    @Column(name = "quotation_no", length = 50)
    private String quotationNo;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_address_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerAddressEntity customerAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_contact_id", referencedColumnName = "id")
    @ToString.Exclude
    private CustomerContactEntity customerContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", referencedColumnName = "employee_id")
    @ToString.Exclude
    private EmployeeEntity sales;

    @Column(name = "co_sales_id", length = 20)
    private String coSalesId;

    @Column(name = "subtotal", precision = 18, scale = 5)
    private BigDecimal subTotal;

    @Column(name = "discount", precision = 18, scale = 5)
    private BigDecimal discount;

    @Column(name = "freight", precision = 18, scale = 5)
    private BigDecimal freight;

    @Column(name = "amount", precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "commission", precision = 18, scale = 5)
    private BigDecimal commission;

    @Column(name = "vat_rate", precision = 6, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "vat", precision = 18, scale = 5)
    private BigDecimal vat;

    @Column(name = "grand_total", precision = 18, scale = 5)
    private BigDecimal grandTotal;

    @Column(name = "paid_total", precision = 18, scale = 5)
    private BigDecimal paidTotal;

    @Column(name = "outstanding_total", precision = 18, scale = 5)
    private BigDecimal outstandingTotal;

    @Column(name = "remark", length = 2000)
    private String remark;

    @Column(name = "rev_no")
    private Integer revNo;

    @Column(name = "customer_name_snapshot", length = 255)
    private String customerNameSnapshot;

    @Column(name = "customer_tax_id_snapshot", length = 50)
    private String customerTaxIdSnapshot;

    @Column(name = "customer_branch_code_snapshot", length = 50)
    private String customerBranchCodeSnapshot;

    @Column(name = "customer_branch_name_snapshot", length = 255)
    private String customerBranchNameSnapshot;

    @Column(name = "customer_address_snapshot", length = 2000)
    private String customerAddressSnapshot;

    @Column(name = "customer_contact_snapshot", length = 255)
    private String customerContactSnapshot;

    @Column(name = "customer_phone_snapshot", length = 100)
    private String customerPhoneSnapshot;

    @OneToOne
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula = @JoinFormula(value = "'CUSTOMER_PAYMENT_TERM'", referencedColumnName = "group_code")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "customer_payment_term", referencedColumnName = "code"))
    })
    @ToString.Exclude
    private SystemConfigEntity customerPaymentTerm;

    @Column(name = "sales_name_snapshot", length = 255)
    private String salesNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<InvoiceDetailEntity> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paymentDate desc, id desc")
    @ToString.Exclude
    private Set<InvoicePaymentEntity> payments = new LinkedHashSet<>();

    @Column(name = "is_required_approve")
    private Boolean requiredApprove;

    @Column(name = "required_approve_reason", columnDefinition = "TEXT")
    private String requiredApproveReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_approve_status", length = 30)
    private UrgentRequestStatus requiredApproveStatus;

    @Column(name = "request_required_approved_by")
    private String requestRequiredApprovedBy;

    @Column(name = "request_required_approved_date")
    private ZonedDateTime requestRequiredApprovedDate;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_date")
    private ZonedDateTime approvedDate;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_date")
    private ZonedDateTime rejectedDate;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    public void addItem(InvoiceDetailEntity item) {
        if (item == null) return;
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceDetailEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setInvoice(null);
    }

    public void addPayment(InvoicePaymentEntity payment) {
        if (payment == null) return;
        payments.add(payment);
        payment.setInvoice(this);
    }

    public void removePayment(InvoicePaymentEntity payment) {
        if (payment == null) return;
        payments.remove(payment);
        payment.setInvoice(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceEntity that)) return false;
        return invoiceNo != null && invoiceNo.equals(that.invoiceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceNo);
    }
}
