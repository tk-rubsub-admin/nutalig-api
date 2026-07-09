package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderDetailDto {
    private Long id;
    private Long salesOrderDetailId;
    private Integer lineNo;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal quantity;
    private Currency supplierCurrency;
    private BigDecimal supplierUnitPrice;
    private BigDecimal exchangeRate;
    private BigDecimal supplierShippingCost;
    private BigDecimal supplierTotalUnitCost;
    private BigDecimal amountSupplierCurrency;
    private BigDecimal amountThb;
    private String imageUrl;
    private Long rfqDetailId;
    private Long rfqTierId;
    private Long quotationDetailId;
    private String shippingMethod;
    private Long supplierQuoteTierId;
}
