package com.nutalig.controller.invoice.request;

import com.nutalig.dto.QuotationCustomerSnapshotDto;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateInvoiceRequest {
    private LocalDate deliveryDate;
    private String customerPaymentTerm;
    private QuotationCustomerSnapshotDto customerSnapshot;
}
