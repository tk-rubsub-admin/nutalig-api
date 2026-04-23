package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class RequestPriceTierDto {

    private Long id;
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal landFreightCost;
    private BigDecimal seaFreightCost;
    private BigDecimal landTotalPrice;
    private BigDecimal seaTotalPrice;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
