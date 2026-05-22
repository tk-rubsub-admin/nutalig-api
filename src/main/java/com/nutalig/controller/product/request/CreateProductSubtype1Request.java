package com.nutalig.controller.product.request;

import lombok.Data;

@Data
public class CreateProductSubtype1Request {

    private String code;
    private String productFamilyCode;
    private String nameTh;
    private String nameEn;
    private Boolean subtype2Required;

}
