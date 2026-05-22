package com.nutalig.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SupplierCapabilityDto {

    private String productFamilyCode;
    private ProductFamilyDto productFamily;
    private boolean coversAllMaterials;
    private List<SupplierCapabilityMaterialDto> materials = new ArrayList<>();
}
