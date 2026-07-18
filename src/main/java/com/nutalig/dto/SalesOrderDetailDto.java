package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderDetailDto {
    private Long id;
    private Integer lineNo;
    private SupplierDto supplier;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String imageUrl;
    private Long rfqDetailId;
    private Long rfqTierId;
    private Long quotationDetailId;
    private String shippingMethod;
    private Currency supplierCurrency;
    private BigDecimal supplierUnitPrice;
    private BigDecimal supplierShippingCost;
    private BigDecimal supplierTotalUnitCost;
    private Long supplierQuoteTierId;
}
