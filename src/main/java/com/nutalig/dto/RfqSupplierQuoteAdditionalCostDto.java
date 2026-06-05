package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqSupplierQuoteAdditionalCostDto {

    private Long id;
    private String description;
    private String unit;
    private String value;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
