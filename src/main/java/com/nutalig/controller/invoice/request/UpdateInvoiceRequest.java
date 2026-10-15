package com.nutalig.controller.invoice.request;

import com.nutalig.dto.QuotationCustomerSnapshotDto;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateInvoiceRequest {
    private LocalDate deliveryDate;
    private String customerPaymentTerm;
    private QuotationCustomerSnapshotDto customerSnapshot;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private BigDecimal unitPrice;
        private BigDecimal quantity;
    }
}
