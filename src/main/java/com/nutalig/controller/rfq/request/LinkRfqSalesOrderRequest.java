package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LinkRfqSalesOrderRequest {
    private String saleOrderId;
    private Long detailId;
    private Long tierId;
    private String shippingMethod;
    private BigDecimal price;
}
