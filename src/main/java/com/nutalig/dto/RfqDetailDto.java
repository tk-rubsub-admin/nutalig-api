package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqDetailDto {

    private Long id;
    private String optionName;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String recommend;
    private BigDecimal commission;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private SupplierDto supplier;
    private List<RfqTierDto> tiers;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private String createdBy;
    private String updatedBy;
}
