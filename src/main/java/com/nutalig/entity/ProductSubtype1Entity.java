package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_subtype1")
@Entity(name = "ProductSubtype1")
public class ProductSubtype1Entity {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "product_family_code")
    private String productFamilyCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_family_code", referencedColumnName = "code", insertable = false, updatable = false)
    private ProductFamilyEntity productFamilyEntity;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "subtype2_required")
    private Boolean subtype2Required;

}
