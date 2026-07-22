package com.nutalig.dto;

import com.nutalig.constant.RfqStatus;
import com.nutalig.constant.UrgentRequestStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqHeaderDto {

    private String id;
    private ZonedDateTime requestedDate;
    private RfqStatus status;
    private String referenceRfqId;
    private RfqReferenceDto referenceRfq;
    private String contactName;
    private String contactPhone;
    private String contactChannel;
    private EmployeeDto sales;
    private CustomerDto customer;
    private SystemConfigDto rfqType;
    private SystemConfigDto orderType;
    private List<RfqPicturesDto> pictures;
    private List<RfqDetailDto> details;
    private List<RfqAdditionalCostDto> additionalCosts;
    private ProductFamilyDto productFamily;
    private ProductSubtype1Dto productSubtype1;
    private ProductSubtype2Dto productSubType2;
    private ProductMaterialDto material;
    private String capacity;
    private BigDecimal targetPrice;
    private List<BigDecimal> requestedMoqs;
    private Boolean urgentRequest;
    private String urgentRequestReason;
    private UrgentRequestStatus urgentRequestStatus;
    private String urgentRequestedBy;
    private ZonedDateTime urgentRequestedDate;
    private String urgentApprovedBy;
    private ZonedDateTime urgentApprovedDate;
    private String urgentRejectedBy;
    private ZonedDateTime urgentRejectedDate;
    private String urgentRejectReason;
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
    private Boolean isAccept;
    private Boolean isCreatedPurchaseOrder;
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
