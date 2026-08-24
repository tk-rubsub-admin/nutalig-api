package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class RfqTierDto {

    private Long id;
    private SupplierDto supplier;
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal commission;
    private Currency currency;
    private String containerSize;
    private BigDecimal landFreightCost;
    private BigDecimal seaFreightCost;
    private Boolean isFcl;
    private Boolean isShareFCL;
    private BigDecimal landTotalPrice;
    private BigDecimal seaTotalPrice;
    private Long supplierQuoteTierId;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
