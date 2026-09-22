package com.nutalig.controller.rfq.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateRequestPriceTierSplitRequest {
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal sellPrice;
    private BigDecimal commission;
    private Currency currency;
    private String shippingMethod;
    private String containerSize;
    private BigDecimal shippingCost;
    private Boolean isFcl;
    private Boolean isShareFCL;
    private String supplierId;
}
