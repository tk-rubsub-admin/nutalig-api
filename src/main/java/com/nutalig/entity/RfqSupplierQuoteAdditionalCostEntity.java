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
@Table(name = "rfq_supplier_quote_additional_cost")
@Entity(name = "RfqSupplierQuoteAdditionalCost")
public class RfqSupplierQuoteAdditionalCostEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", referencedColumnName = "id", nullable = false)
    private RfqSupplierQuoteEntity supplierQuote;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "value")
    private String value;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
