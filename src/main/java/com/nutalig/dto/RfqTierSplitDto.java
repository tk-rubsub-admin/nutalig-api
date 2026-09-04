package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class RfqTierSplitDto {

    private Long id;
    private SupplierDto supplier;
    private BigDecimal quantity;
    private BigDecimal sellPrice;
    private BigDecimal commission;
    private Currency currency;
    private String shippingMethod;
    private String containerSize;
    private BigDecimal landFreightCost;
    private BigDecimal landFreightQty;
    private BigDecimal seaFreightQty;
    private BigDecimal seaFreightCost;
    private Boolean isFcl;
    private Boolean isShareFCL;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
