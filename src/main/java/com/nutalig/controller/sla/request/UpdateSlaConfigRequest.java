package com.nutalig.controller.sla.request;

import com.nutalig.constant.SlaDayType;
import com.nutalig.constant.Status;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateSlaConfigRequest {

    private String slaName;
    private Integer targetDays;
    private SlaDayType dayType;
    private Status status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
