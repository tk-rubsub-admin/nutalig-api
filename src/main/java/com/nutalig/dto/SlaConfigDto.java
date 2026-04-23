package com.nutalig.dto;

import com.nutalig.constant.SlaDayType;
import com.nutalig.constant.Status;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class SlaConfigDto {

    private String slaCode;
    private String slaName;
    private Integer targetDays;
    private SlaDayType dayType;
    private Status status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private ZonedDateTime createdDate;
    private String createdBy;
    private ZonedDateTime updatedDate;
    private String updatedBy;
}
