package com.nutalig.entity;

import com.nutalig.constant.Currency;
import com.nutalig.constant.QuotationStatus;
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
@Table(name = "quotations")
@ToString(onlyExplicitlyIncluded = true)
public class QuotationEntity extends AuditDateEntity {

    @Id
    @Column(name = "quotation_no", unique = true, nullable = false, length = 50)
    @ToString.Include
    private String quotationNo;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private QuotationStatus status;

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

    @Column(name = "customer_name_snapshot") private String customerNameSnapshot;
    @Column(name = "customer_tax_id_snapshot") private String customerTaxIdSnapshot;
    @Column(name = "customer_branch_code_snapshot") private String customerBranchCodeSnapshot;
    @Column(name = "customer_branch_name_snapshot") private String customerBranchNameSnapshot;
    @Column(name = "customer_address_snapshot", length = 2000) private String customerAddressSnapshot;
    @Column(name = "customer_contact_snapshot") private String customerContactSnapshot;
    @Column(name = "customer_phone_snapshot") private String customerPhoneSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", referencedColumnName = "employee_id")
    @ToString.Exclude
    private EmployeeEntity sales;

    @Column(name = "rfq_id", length = 50)
    private String rfqId;

    /** RFQs consolidated into this quotation. rfqId remains the primary legacy reference. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quotation_rfq_reference", joinColumns = @JoinColumn(name = "quotation_no"))
    @Column(name = "rfq_id", nullable = false, length = 50)
    private Set<String> rfqIds = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private RfqHeaderEntity rfq;

    @Column(name = "reference_rfq_id", length = 50)
    private String referenceRfqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_rfq_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private RfqHeaderEntity referenceRfq;

    @Column(name = "co_sales_id", length = 20)
    private String coSalesId;

    @Column(name = "subtotal", precision = 18, scale = 5)
    private BigDecimal subTotal;

    @Column(name = "discount", precision = 18, scale = 5)
    private BigDecimal discount;

    @Column(name = "freight", precision = 18, scale = 5)
    private BigDecimal freight;

    @Column(name = "vat", precision = 18, scale = 5)
    private BigDecimal vat;

    @Column(name = "grand_total", precision = 18, scale = 5)
    private BigDecimal grandTotal;

    @Column(name = "vat_rate", precision = 6, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "remark", length = 2000)
    private String remark;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<QuotationDetailEntity> items = new LinkedHashSet<>();

    @Column(name = "created_by")
    @ToString.Include
    private String createdBy;

    @Column(name = "updated_by")
    @ToString.Include
    private String updatedBy;

    @Column(name = "rev_no")
    private Integer revNo;

    @Column(name = "is_show_summary")
    private Boolean isShowSummary;

    @Column(name = "shipping")
    private String shipping;

    public void addItem(QuotationDetailEntity item) {
        if (item == null) return;
        items.add(item);
        item.setQuotation(this);
    }

    public void removeItem(QuotationDetailEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setQuotation(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuotationEntity that)) return false;
        return quotationNo != null && quotationNo.equals(that.quotationNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quotationNo);
    }
}
