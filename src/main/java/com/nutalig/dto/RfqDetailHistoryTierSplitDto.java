package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RfqDetailHistoryTierSplitDto {
    private Long sourceTierSplitId;
    private String supplierId;
    private BigDecimal quantity;
    private BigDecimal sellPrice;
    private BigDecimal commission;
    private Currency currency;
    private BigDecimal landFreightCost;
    private BigDecimal landFreightQty;
    private BigDecimal seaFreightQty;
    private BigDecimal seaFreightCost;
    private Boolean isFcl;
    private Boolean isShareFCL;
}
