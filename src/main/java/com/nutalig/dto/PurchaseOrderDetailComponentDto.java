package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderDetailComponentDto {
    private Long id;
    private Long sourceQuoteDetailPackageId;
    private String componentCode;
    private String componentName;
    private String specification;
    private BigDecimal quantityPerItem;
    private BigDecimal totalQuantity;
    private String unit;
    private String remark;
    private Integer sortOrder;
}
