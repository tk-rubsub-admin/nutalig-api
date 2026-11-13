package com.nutalig.controller.invoice.request;

import lombok.Data;

@Data
public class RejectInvoicePaymentRequest {
    private String reason;
}
