package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.ReceiptStatus;
import com.nutalig.constant.ReceiptType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "receipt")
@ToString(onlyExplicitlyIncluded = true)
public class ReceiptEntity extends AuditDateEntity {

    @Id
    @Column(name = "receipt_no", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String receiptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 40)
    private ReceiptType receiptType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceiptStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_no", referencedColumnName = "invoice_no", nullable = false)
    @ToString.Exclude
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_payment_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private InvoicePaymentEntity invoicePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_no", referencedColumnName = "sales_order_no")
    @ToString.Exclude
    private SalesOrderEntity salesOrder;
    @Column(name = "quotation_no", length = 50)
    private String quotationNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(name = "paid_date", nullable = false)
    private ZonedDateTime paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
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

    @Column(name = "subtotal", precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount", precision = 18, scale = 2)
    private BigDecimal discount;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "vat_rate", precision = 10, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "vat", precision = 18, scale = 2)
    private BigDecimal vat;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "cheque_bank", length = 255)
    private String chequeBank;

    @Column(name = "cheque_no", length = 100)
    private String chequeNo;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "cheque_branch", length = 255)
    private String chequeBranch;

    @Column(name = "slip_file_name", length = 255)
    private String slipFileName;

    @Column(name = "slip_file_url", length = 500)
    private String slipFileUrl;

    @Column(name = "remark", length = 2000)
    private String remark;

    @Column(name = "rev_no")
    private Integer revNo;

    @Column(name = "customer_name_snapshot", length = 255)
    private String customerNameSnapshot;

    @Column(name = "customer_tax_id_snapshot", length = 50)
    private String customerTaxIdSnapshot;

    @Column(name = "customer_address_snapshot", length = 2000)
    private String customerAddressSnapshot;

    @Column(name = "customer_contact_snapshot", length = 255)
    private String customerContactSnapshot;

    @Column(name = "customer_phone_snapshot", length = 100)
    private String customerPhoneSnapshot;

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

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<ReceiptDetailEntity> items = new LinkedHashSet<>();

    public void addItem(ReceiptDetailEntity item) {
        if (item == null) return;
        items.add(item);
        item.setReceipt(this);
    }

    public void removeItem(ReceiptDetailEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setReceipt(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReceiptEntity that)) return false;
        return receiptNo != null && receiptNo.equals(that.receiptNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptNo);
    }
}
