package com.nutalig.controller.rfq.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRequestPriceHeaderRequest {

    private String contactName;
    private String contactPhone;
    private String salesId;
    private String customerId;
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
    private Boolean urgentRequest;
    private String urgentRequestReason;
    private String description;
    private List<MultipartFile> pictures;
    private List<MultipartFile> attachments;
}
