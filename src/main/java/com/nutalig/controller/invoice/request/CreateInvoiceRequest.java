package com.nutalig.controller.invoice.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateInvoiceRequest {
    private String salesOrderNo;
    private LocalDate docDate;
    private LocalDate dueDate;
    private String remark;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal amount;
    private BigDecimal vat;
    private BigDecimal grandTotal;
}
