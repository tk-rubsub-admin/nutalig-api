package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.util.List;

@Data
public class AddSupplierMaterialCapabilityRequest {

    private List<AddSupplierMaterialCapability> capabilities;
}
