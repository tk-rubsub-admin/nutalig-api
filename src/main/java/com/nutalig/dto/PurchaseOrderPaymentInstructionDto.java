package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderPaymentInstructionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class PurchaseOrderPaymentInstructionDto {
    private Long id;
    private Long scheduleId;
    private Integer installmentNo;
    private BigDecimal amount;
    private BigDecimal amountThb;
    private Currency currency;
    private BigDecimal exchangeRate;
    private LocalDate plannedPaymentDate;
    private BigDecimal paidAmount;
    private BigDecimal paidAmountThb;
    private ZonedDateTime confirmedPaidDate;
    private PurchaseOrderPaymentInstructionStatus status;
    private String remark;
    private UserDto createdBy;
    private ZonedDateTime createdDate;
    private UserDto updatedBy;
    private ZonedDateTime updatedDate;
}
