package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RequestPriceDetailDto {

    private Long id;
    private String optionName;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private List<RequestPriceTierDto> tiers;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private String createdBy;
    private String updatedBy;
}
