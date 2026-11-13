package com.nutalig.dto;

import com.nutalig.constant.InvoicePaymentStatus;
import com.nutalig.constant.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class InvoicePendingPaymentDto {
    private Long paymentId;
    private String invoiceNo;
    private String salesOrderNo;
    private String customerId;
    private String customerName;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private BigDecimal unallocatedAmount;
    private PaymentMethod paymentMethod;
    private ZonedDateTime paymentDate;
    private ZonedDateTime createdDate;
    private InvoicePaymentStatus status;
    private String slipFileName;
    private String slipFileUrl;
}
