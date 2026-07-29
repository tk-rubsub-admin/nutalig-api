package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateRequestPriceHeaderRequest {

    private String contactName;
    private String contactPhone;
    private String contactChannel;
    private String salesId;
    private String procurementId;
    private String referenceRfqId;
    private String rfqTypeCode;
    private String orderTypeCode;
    private String shippingMethod;
    private String productFamily;
    private String productUsage;
    private String systemMechanic;
    private String material;
    private String capacity;
    private BigDecimal targetPrice;
    private List<BigDecimal> requestedMoqs;
    private Boolean requestSample;
    private String description;
    private String requestInformation;
    private String note;
}
