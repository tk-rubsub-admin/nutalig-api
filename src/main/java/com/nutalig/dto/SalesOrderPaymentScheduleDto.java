package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderPaymentType;
import com.nutalig.constant.SalesOrderPaymentScheduleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesOrderPaymentScheduleDto {
    private Long id;
    private String salesOrderNo;
    private Integer installmentNo;
    private Integer installmentCount;
    private PurchaseOrderPaymentType paymentType;
    private BigDecimal percentage;
    private BigDecimal expectedSubTotal;
    private BigDecimal expectedVat;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private SalesOrderPaymentScheduleStatus status;
    private LocalDate dueDate;
    private String invoiceNo;
}
