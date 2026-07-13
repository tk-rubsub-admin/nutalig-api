package com.nutalig.controller.rfq.request;

import com.nutalig.constant.Currency;
import lombok.Data;

@Data
public class ExtractRfqSupplierQuoteRequest {

    private String supplierId;
    private String inquiryId;
    private Currency defaultCurrency;
    private String supplierMessage;
}
