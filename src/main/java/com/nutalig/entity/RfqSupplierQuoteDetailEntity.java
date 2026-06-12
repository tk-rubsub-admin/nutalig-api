package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "rfq_supplier_quote_detail")
@Entity(name = "RfqSupplierQuoteDetail")
public class RfqSupplierQuoteDetailEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", referencedColumnName = "id", nullable = false)
    private RfqSupplierQuoteEntity supplierQuote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_detail_id", referencedColumnName = "id")
    private RfqDetailEntity requestPriceDetail;

    @Column(name = "option_name", length = 255)
    private String optionName;

    @Column(name = "spec", columnDefinition = "TEXT", nullable = false)
    private String spec;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @OneToMany(mappedBy = "quoteDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqSupplierQuoteTierEntity> tiers = new ArrayList<>();

    public void addTier(RfqSupplierQuoteTierEntity tier) {
        tiers.add(tier);
        tier.setQuoteDetail(this);
    }
}
