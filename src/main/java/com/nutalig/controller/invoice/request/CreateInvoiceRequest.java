package com.nutalig.controller.invoice.request;

import com.nutalig.dto.QuotationCustomerSnapshotDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateInvoiceRequest {
    private String salesOrderNo;
    private LocalDate docDate;
    private LocalDate dueDate;
    private LocalDate deliveryDate;
    private String remark;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal amount;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private String customerPaymentTerm;
    private QuotationCustomerSnapshotDto customerSnapshot;
}
