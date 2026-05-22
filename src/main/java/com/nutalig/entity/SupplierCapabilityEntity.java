package com.nutalig.entity;

import com.nutalig.constant.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "supplier_product_capability")
@Entity(name = "SupplierCapability")
public class SupplierCapabilityEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "supplierCapabilityIdGenerator")
    @GenericGenerator(name = "supplierCapabilityIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-SPC"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", referencedColumnName = "id", nullable = false)
    private SupplierEntity supplier;

    @Column(name = "product_family_code", nullable = false)
    private String productFamilyCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_family_code", referencedColumnName = "code", insertable = false, updatable = false)
    private ProductFamilyEntity productFamily;

    @Column(name = "product_material_code")
    private String productMaterialCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "product_material_code", referencedColumnName = "code", insertable = false, updatable = false),
            @JoinColumn(name = "product_family_code", referencedColumnName = "product_family_code", insertable = false, updatable = false)
    })
    private ProductMaterialEntity productMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
}
