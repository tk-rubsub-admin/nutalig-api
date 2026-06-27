package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqSupplierQuoteDetailPackageDto {

    private Long id;
    private String packageDimension;
    private String packageName;
    private String packageWeight;
    private String packageCapacity;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
