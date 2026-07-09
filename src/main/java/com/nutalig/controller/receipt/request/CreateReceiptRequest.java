package com.nutalig.controller.receipt.request;

import com.nutalig.constant.ReceiptType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReceiptRequest {
    private String invoiceNo;
    private Long invoicePaymentId;
    private ReceiptType receiptType;
    private LocalDate docDate;
    private String remark;
}
