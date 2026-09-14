package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Table(name = "product_subtype1")
@Entity(name = "ProductSubtype1")
public class ProductSubtype1Entity {

    @Id
    @GeneratedValue(generator = "productSubtype1IdGenerator")
    @GenericGenerator(name = "productSubtype1IdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "PST"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d"),
                    @org.hibernate.annotations.Parameter(name = "separator", value = "")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
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
