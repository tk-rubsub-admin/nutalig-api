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
@Table(name = "rfq_supplier_quote_detail_package")
@Entity(name = "RfqSupplierQuoteDetailPackage")
public class RfqSupplierQuoteDetailPackageEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_detail_id", referencedColumnName = "id", nullable = false)
    private RfqSupplierQuoteDetailEntity quoteDetail;

    @Column(name = "package_dimension", columnDefinition = "TEXT")
    private String packageDimension;

    @Column(name = "package_name", length = 255)
    private String packageName;

    @Column(name = "package_weight", columnDefinition = "TEXT")
    private String packageWeight;

    @Column(name = "package_capacity", columnDefinition = "TEXT")
    private String packageCapacity;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
