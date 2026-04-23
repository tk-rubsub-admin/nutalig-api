package com.nutalig.controller.rfq.request;

import lombok.Data;

@Data
public class UpdateRequestPriceHeaderRequest {

    private String orderTypeCode;
    private String productFamily;
    private String productUsage;
    private String systemMechanic;
    private String material;
    private String capacity;
    private String description;
}
