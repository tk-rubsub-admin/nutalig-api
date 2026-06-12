package com.nutalig.dto;

import com.nutalig.constant.RfqStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RequestPriceHeaderDto {

    private String id;
    private ZonedDateTime requestedDate;
    private RfqStatus status;
    private String contactName;
    private String contactPhone;
    private EmployeeDto sales;
    private CustomerDto customer;
    private SystemConfigDto orderType;
    private List<RequestPricePicturesDto> pictures;
    private List<RequestPriceDetailDto> details;
    private List<RequestPriceAdditionalCostDto> additionalCosts;
    private ProductFamilyDto productFamily;
    private ProductSubtype1Dto productSubtype1;
    private ProductSubtype2Dto productSubType2;
    private ProductMaterialDto material;
    private String capacity;
    private String description;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private EmployeeDto procurement;
    private ZonedDateTime slaDate;
    private ZonedDateTime quotedDate;
    private String quotationNo;
    private String saleOrderId;
    private Long confirmedDetailId;
    private Long confirmedTierId;
    private String confirmedShippingMethod;
    private java.math.BigDecimal confirmedPrice;
    private ZonedDateTime confirmedDate;
}
