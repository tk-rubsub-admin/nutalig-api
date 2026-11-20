package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseOrderReceiptDetailDto {
    private Long id;
    private Long deliveryDetailId;
    private Long purchaseOrderDetailId;
    private Long purchaseOrderDetailComponentId;
    private String productName;
    private String componentName;
    private BigDecimal shippedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal damagedQuantity;
    private BigDecimal shortageQuantity;
    private BigDecimal excessQuantity;
    private BigDecimal receivedQuantityToDate;
    private BigDecimal remainingQuantity;
    private String lotNo;
    private LocalDate expiryDate;
    private Long warehouseLocationId;
    private String remark;
}
