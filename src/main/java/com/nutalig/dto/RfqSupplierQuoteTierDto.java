package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class RfqSupplierQuoteTierDto {

    private Long id;
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal shippingCost;
    private Currency currency;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
