package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "product_subtype2")
@Entity(name = "ProductSubtype2")
public class ProductSubtype2Entity {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "product_subtype1_code")
    private String productSubtype1Code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_subtype1_code", referencedColumnName = "code", insertable = false, updatable = false)
    private ProductSubtype1Entity productSubtype1Entity;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

}
