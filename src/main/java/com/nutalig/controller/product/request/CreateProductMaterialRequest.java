package com.nutalig.controller.product.request;

import lombok.Data;

@Data
public class CreateProductMaterialRequest {

    private String productFamilyCode;
    private String nameTh;
    private String nameEn;
}
