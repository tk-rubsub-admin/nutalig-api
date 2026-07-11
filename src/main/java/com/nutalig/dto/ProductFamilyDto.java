package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductFamilyDto {
    private String code;
    private String nameTh;
    private String nameEn;
    private Boolean isActive;
    private List<ProductMaterialDto> materialList;
    private List<ProductSubtype1Dto> subtype1List;
}
