package com.nutalig.controller.invoice.request;

import com.nutalig.constant.InvoiceStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SearchInvoiceRequest {
    private String invoiceNo;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private String customerId;
    private String salesId;
    private InvoiceStatus status;
    private List<InvoiceStatus> statuses;
    private String keyword;
}
