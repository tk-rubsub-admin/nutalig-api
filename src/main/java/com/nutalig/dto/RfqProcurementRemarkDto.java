package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqProcurementRemarkDto {

    private String remark;
    private String createdBy;
    private ZonedDateTime createdDate;
}
