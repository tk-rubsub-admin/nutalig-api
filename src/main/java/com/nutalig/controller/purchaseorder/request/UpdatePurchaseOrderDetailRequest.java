package com.nutalig.controller.purchaseorder.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePurchaseOrderDetailRequest {
    private Long id;
    private Long salesOrderDetailId;
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
    private String imageUrl;
    private Long rfqDetailId;
    private Long rfqTierId;
    private Long quotationDetailId;
    private String shippingMethod;
    private Long supplierQuoteTierId;
}
