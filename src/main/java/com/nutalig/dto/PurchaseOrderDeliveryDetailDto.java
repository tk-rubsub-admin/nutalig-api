package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderDeliveryDetailDto {
    private Long id;
    private Long purchaseOrderDetailId;
    private Long purchaseOrderDetailComponentId;
    private String productName;
    private String componentName;
    private BigDecimal quantity;
    private BigDecimal cartonCount;
    private BigDecimal piecesPerCarton;
    private BigDecimal netWeightKg;
    private BigDecimal grossWeightKg;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal cbmM3;
    private Integer sortOrder;
}
