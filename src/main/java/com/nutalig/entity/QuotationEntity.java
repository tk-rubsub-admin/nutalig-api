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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", referencedColumnName = "sales_id")
    @ToString.Exclude
    private SalesEntity sales;

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

    @Column(name = "vat_rate", precision = 6, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "remark", length = 2000)
    private String remark;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    @ToString.Exclude
    private Set<QuotationDetailEntity> items = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    @ToString.Exclude
    private UserEntity updatedBy;

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