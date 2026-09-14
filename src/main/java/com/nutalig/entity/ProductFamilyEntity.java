package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Table(name = "product_family")
@Entity(name = "ProductFamily")
public class ProductFamilyEntity {

    @Id
    @GeneratedValue(generator = "productFamilyIdGenerator")
    @GenericGenerator(name = "productFamilyIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "PF"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%03d"),
                    @org.hibernate.annotations.Parameter(name = "separator", value = "")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @Column(name = "code")
    private String code;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "is_active")
    private Boolean isActive;

}
