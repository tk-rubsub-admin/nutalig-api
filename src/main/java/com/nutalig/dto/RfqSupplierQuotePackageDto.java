package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqSupplierQuotePackageDto {

    private Long id;
    private String packageName;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
