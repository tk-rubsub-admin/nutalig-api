package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderPaymentScheduleStatus;
import com.nutalig.constant.PurchaseOrderPaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseOrderPaymentScheduleDto {
    private Long id;
    private Integer installmentNo;
    private PurchaseOrderPaymentType paymentType;
    private BigDecimal percentage;
    private BigDecimal expectedAmount;
    private BigDecimal expectedAmountThb;
    private BigDecimal paidAmount;
    private BigDecimal paidAmountThb;
    private BigDecimal pendingAmount;
    private BigDecimal pendingAmountThb;
    private BigDecimal outstandingAmount;
    private BigDecimal outstandingAmountThb;
    private PurchaseOrderPaymentScheduleStatus status;
    private LocalDate dueDate;
}
