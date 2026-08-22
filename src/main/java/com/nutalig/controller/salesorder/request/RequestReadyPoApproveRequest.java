package com.nutalig.controller.salesorder.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestReadyPoApproveRequest {

    private String reason;
    private LocalDate paymentScheduleDate;
}
