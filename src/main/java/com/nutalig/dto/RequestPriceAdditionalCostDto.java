package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RequestPriceAdditionalCostDto {

    private Long id;
    private SupplierDto supplier;
    private String description;
    private String unit;
    private String value;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
