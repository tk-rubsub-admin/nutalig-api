package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqSupplierQuoteDetailDto {

    private Long id;
    private Long rfqDetailId;
    private String optionName;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private List<RfqSupplierQuoteTierDto> tiers;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
