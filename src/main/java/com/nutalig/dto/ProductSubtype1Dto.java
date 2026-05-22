package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductSubtype1Dto {
    private String code;
    private String productFamilyCode;
    private String nameTh;
    private String nameEn;
    private Boolean subtype2Required;
    private List<ProductSubtype2Dto> subtype2List;
}
