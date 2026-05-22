package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.util.List;

@Data
public class AddSupplierMaterialCapability {
    private String productFamilyCode;
    private List<String> productMaterialCode;
}
