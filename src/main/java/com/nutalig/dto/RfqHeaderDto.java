package com.nutalig.dto;

import com.nutalig.constant.RfqStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqHeaderDto {

    private String id;
    private ZonedDateTime requestedDate;
    private RfqStatus status;
    private String contactName;
    private String contactPhone;
    private EmployeeDto sales;
    private CustomerDto customer;
    private SystemConfigDto orderType;
    private List<RfqPicturesDto> pictures;
    private List<RfqDetailDto> details;
    private List<RfqAdditionalCostDto> additionalCosts;
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
    private String shippingMethod;
    private String requestInformation;
    private String remark;
    private List<RfqStatusTimelineDto> rfqStatusTimeline;
    private Long confirmedDetailId;
    private Long confirmedTierId;
    private String confirmedShippingMethod;
    private java.math.BigDecimal confirmedPrice;
    private ZonedDateTime confirmedDate;
}
