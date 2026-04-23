package com.nutalig.dto;

import com.nutalig.constant.RFQStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RequestPriceHeaderDto {

    private String id;
    private ZonedDateTime requestedDate;
    private RFQStatus status;
    private String contactName;
    private String contactPhone;
    private EmployeeDto sales;
    private CustomerDto customer;
    private SystemConfigDto orderType;
    private List<RequestPricePicturesDto> pictures;
    private List<RequestPriceDetailDto> details;
    private List<RequestPriceAdditionalCostDto> additionalCosts;
    private ProductFamilyDto productFamily;
    private String productUsage;
    private String systemMechanic;
    private String material;
    private String capacity;
    private String description;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private EmployeeDto procurement;
    private ZonedDateTime slaDate;
    private ZonedDateTime quotedDate;
}
