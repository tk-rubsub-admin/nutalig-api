package com.nutalig.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseOrderPackageSnapshotDto {
    private Long id;
    private Long sourcePackageId;
    private String packageName;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private BigDecimal heightCm;
    private BigDecimal capacityQty;
    private Long cartonCount;
    private BigDecimal cbmPerCarton;
    private BigDecimal totalCbm;
    private Boolean selectedForCalculation;
    private Integer sortOrder;
}
