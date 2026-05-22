package com.nutalig.controller.product.request;

import lombok.Data;

@Data
public class UpdateProductSubtype1Request {

    private String productFamilyCode;
    private String nameTh;
    private String nameEn;
    private Boolean subtype2Required;

}
