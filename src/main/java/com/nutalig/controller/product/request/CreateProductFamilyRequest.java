package com.nutalig.controller.product.request;

import lombok.Data;

@Data
public class CreateProductFamilyRequest {

    private String nameTh;
    private String nameEn;
    private Boolean isActive;
}
