package com.nutalig.entity.id;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductMaterialId implements Serializable {
    private String code;
    private String productFamilyCode;
}
