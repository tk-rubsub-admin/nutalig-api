package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRequestPriceAdditionalCostRequest {

    private String description;
    private String unit;
    private String value;
    private Integer sortOrder;
}
