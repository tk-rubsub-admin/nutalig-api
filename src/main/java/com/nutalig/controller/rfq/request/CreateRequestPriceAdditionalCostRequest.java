package com.nutalig.controller.rfq.request;

import lombok.Data;

@Data
public class CreateRequestPriceAdditionalCostRequest {

    private String description;
    private String unit;
    private String value;
    private Integer sortOrder;
}
