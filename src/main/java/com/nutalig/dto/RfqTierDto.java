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
    private BigDecimal targetPrice;
    private BigDecimal commission;
    private Currency currency;
    private String shippingMethod;
    private String containerSize;
    private BigDecimal shippingCost;
    private Boolean isFcl;
    private Boolean isShareFCL;
    private BigDecimal totalPrice;
    private Long supplierQuoteTierId;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
