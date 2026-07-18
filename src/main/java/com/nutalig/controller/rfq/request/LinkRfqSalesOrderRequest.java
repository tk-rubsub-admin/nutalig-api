package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LinkRfqSalesOrderRequest {
    private String saleOrderId;
    private Long detailId;
    private Long tierId;
    private String shippingMethod;
    private BigDecimal price;
    private List<Selection> selections;

    @Data
    public static class Selection {
        private Long detailId;
        private Long tierId;
        private String shippingMethod;
        private BigDecimal price;
    }
}
