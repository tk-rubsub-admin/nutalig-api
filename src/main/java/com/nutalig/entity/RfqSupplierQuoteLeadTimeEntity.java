package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "rfq_supplier_quote_lead_time")
@Entity(name = "RfqSupplierQuoteLeadTime")
public class RfqSupplierQuoteLeadTimeEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_quote_id", referencedColumnName = "id", nullable = false)
    private RfqSupplierQuoteEntity supplierQuote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_time_code", referencedColumnName = "code", nullable = false)
    private LeadTimeConfigEntity leadTimeConfig;

    @Column(name = "lead_time_day_min", nullable = false)
    private Integer leadTimeDayMin;

    @Column(name = "lead_time_day_max", nullable = false)
    private Integer leadTimeDayMax;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
