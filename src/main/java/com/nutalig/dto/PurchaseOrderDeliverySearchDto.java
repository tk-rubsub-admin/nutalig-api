package com.nutalig.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PurchaseOrderDeliverySearchDto extends PurchaseOrderDeliveryDto {
    private String salesOrderNo;
    private String supplierId;
    private String supplierName;
    private BigDecimal totalQuantity;
    private BigDecimal totalCartons;
    private BigDecimal totalCbm;
}
