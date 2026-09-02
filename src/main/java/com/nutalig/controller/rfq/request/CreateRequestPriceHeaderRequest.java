package com.nutalig.controller.rfq.request;

import lombok.Data;
import com.nutalig.dto.RequestedMoqDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateRequestPriceHeaderRequest {

    private String contactName;
    private String contactPhone;
    private String contactChannel;
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
    private List<RequestedMoqDto> requestedMoqs;
    private Boolean requestSample;
    private Boolean urgentRequest;
    private String urgentRequestReason;
    private String description;
    private String note;
    private List<MultipartFile> pictures;
    private List<MultipartFile> attachments;
}
