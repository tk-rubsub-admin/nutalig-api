package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqSupplierQuoteLeadTimeDto {
    private Long id;
    private String leadTimeCode;
    private LeadTimeConfigDto leadTimeConfig;
    private Integer leadTimeDayMin;
    private Integer leadTimeDayMax;
    private String remark;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
