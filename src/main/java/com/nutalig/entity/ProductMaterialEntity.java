package com.nutalig.entity;

import com.nutalig.entity.id.ProductMaterialId;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Table(name = "product_material")
@Entity(name = "ProductMaterial")
@IdClass(ProductMaterialId.class)
public class ProductMaterialEntity {


    @Id
    @GeneratedValue(generator = "productMaterialIdGenerator")
    @GenericGenerator(name = "productMaterialIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "PMAT"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d"),
                    @org.hibernate.annotations.Parameter(name = "separator", value = "")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
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
