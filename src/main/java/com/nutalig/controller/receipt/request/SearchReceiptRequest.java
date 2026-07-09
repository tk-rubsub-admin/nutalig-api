package com.nutalig.controller.receipt.request;

import com.nutalig.constant.ReceiptStatus;
import com.nutalig.constant.ReceiptType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SearchReceiptRequest {
    private String receiptNo;
    private String invoiceNo;
    private String customerId;
    private String salesId;
    private ReceiptType receiptType;
    private ReceiptStatus status;
    private List<ReceiptStatus> statuses;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private String keyword;
}
