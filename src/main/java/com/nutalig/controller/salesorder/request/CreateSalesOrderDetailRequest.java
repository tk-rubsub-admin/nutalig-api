package com.nutalig.controller.salesorder.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSalesOrderDetailRequest {
    private String supplierId;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private String imageUrl;
    private Long rfqDetailId;
    private Long rfqTierId;
    private Long quotationDetailId;
    private String shippingMethod;
    private Currency supplierCurrency;
    private BigDecimal supplierUnitPrice;
    private BigDecimal exchangeRate;
    private BigDecimal supplierShippingCost;
    private BigDecimal supplierTotalUnitCost;
    private Long supplierQuoteTierId;
}
