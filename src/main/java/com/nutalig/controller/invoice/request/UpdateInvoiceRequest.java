package com.nutalig.controller.invoice.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateInvoiceRequest {
    private LocalDate deliveryDate;
}
