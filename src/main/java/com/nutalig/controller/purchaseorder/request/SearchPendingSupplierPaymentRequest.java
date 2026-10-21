package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SearchPendingSupplierPaymentRequest {
    private String supplierId;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private String keyword;
}
