package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderDetailComponentRequest {
    private Long sourceQuoteDetailPackageId;
    private String componentCode;
    private String componentName;
    private String specification;
    private BigDecimal quantityPerItem;
    private String unit;
    private String remark;
    private Integer sortOrder;
}
