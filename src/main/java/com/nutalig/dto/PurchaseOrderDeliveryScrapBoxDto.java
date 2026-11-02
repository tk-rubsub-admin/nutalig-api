package com.nutalig.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseOrderDeliveryScrapBoxDto {
    private Long id;
    private BigDecimal netWeightKg;
    private BigDecimal grossWeightKg;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal cbmM3;
    private Integer sortOrder;
}
