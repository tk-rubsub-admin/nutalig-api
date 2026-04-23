package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RFQStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class CreateRequestPriceHeaderRequest {

    private String contactName;
    private String contactPhone;
    private String salesId;
    private String customerId;
    private String procurementId;
    private String orderTypeCode;
    private String productFamily;
    private String productUsage;
    private String systemMechanic;
    private String material;
    private String capacity;
    private String description;
    private List<MultipartFile> pictures;
}
