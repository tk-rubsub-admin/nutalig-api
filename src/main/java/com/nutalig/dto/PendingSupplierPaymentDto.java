package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderPaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PendingSupplierPaymentDto {
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String supplierId;
    private String supplierName;
    private LocalDate docDate;
    private Currency currency;
    private BigDecimal grandTotal;
    private BigDecimal grandTotalThb;
    private Long scheduleId;
    private Integer installmentNo;
    private PurchaseOrderPaymentType paymentType;
    private BigDecimal percentage;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal payableAmount;
    private BigDecimal expectedAmountThb;
    private BigDecimal paidAmountThb;
    private BigDecimal pendingAmountThb;
    private BigDecimal outstandingAmountThb;
    private BigDecimal payableAmountThb;
    private LocalDate dueDate;
}
