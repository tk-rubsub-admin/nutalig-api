package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestSpecialPriceRequest {
    private BigDecimal targetPrice;
}
