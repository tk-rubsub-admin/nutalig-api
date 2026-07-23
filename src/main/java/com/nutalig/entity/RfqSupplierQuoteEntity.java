package com.nutalig.entity;

import com.nutalig.constant.RfqSupplierQuoteStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "rfq_supplier_quote",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rfq_supplier_quote_rfq_supplier_revision",
                columnNames = {"rfq_header_id", "supplier_id", "revision_no"}
        )
)
@Entity(name = "RfqSupplierQuote")
public class RfqSupplierQuoteEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "rfqSupplierQuoteIdGenerator")
    @GenericGenerator(name = "rfqSupplierQuoteIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-RFQ-Q"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_header_id", referencedColumnName = "id", nullable = false)
    private RfqHeaderEntity requestPriceHeader;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", nullable = false)
    private SupplierEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", referencedColumnName = "id")
    private RfqSupplierInquiryEntity inquiry;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RfqSupplierQuoteStatus status;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToMany(mappedBy = "supplierQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqSupplierQuoteDetailEntity> details = new ArrayList<>();

    @OneToMany(mappedBy = "supplierQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqSupplierQuoteAdditionalCostEntity> additionalCosts = new ArrayList<>();

    @OneToMany(mappedBy = "supplierQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqSupplierQuoteLeadTimeEntity> leadTimes = new ArrayList<>();

    public void addDetail(RfqSupplierQuoteDetailEntity detail) {
        details.add(detail);
        detail.setSupplierQuote(this);
    }

    public void addAdditionalCost(RfqSupplierQuoteAdditionalCostEntity additionalCost) {
        additionalCosts.add(additionalCost);
        additionalCost.setSupplierQuote(this);
    }

    public void addLeadTime(RfqSupplierQuoteLeadTimeEntity leadTime) {
        leadTimes.add(leadTime);
        leadTime.setSupplierQuote(this);
    }
}
