package com.nutalig.controller.purchaseorder.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePurchaseOrderPaymentInstructionRequest {
    private Long scheduleId;
    private BigDecimal amount;
    private BigDecimal amountThb;
    private BigDecimal exchangeRate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedPaymentDate;

    private String remark;
}
