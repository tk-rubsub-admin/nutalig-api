package com.nutalig.controller.rfq.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateRequestPriceTierRequest {
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal commission;
    private Currency currency;
    private BigDecimal landFreightCost;
    private BigDecimal seaFreightCost;
    private Boolean isFcl;
    private BigDecimal landTotalPrice;
    private BigDecimal seaTotalPrice;
    private Long supplierQuoteTierId;
    private Integer sortOrder;
    private String supplierId;
}
