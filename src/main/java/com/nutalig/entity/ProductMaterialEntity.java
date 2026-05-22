package com.nutalig.entity;

import com.nutalig.entity.id.ProductMaterialId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_material")
@Entity(name = "ProductMaterial")
@IdClass(ProductMaterialId.class)
public class ProductMaterialEntity {

    @Id
    @Column(name = "code")
    private String code;

    @Id
    @Column(name = "product_family_code")
    private String productFamilyCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_family_code", referencedColumnName = "code", insertable = false, updatable = false)
    private ProductFamilyEntity productFamilyEntity;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;
}
