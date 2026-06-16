package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.util.List;

@Data
public class SuggestSupplierCapabilitiesRequest {

    private String supplierId;
    private List<AddSupplierMaterialCapability> capabilities;
}
