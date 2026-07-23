package com.nutalig.dto;

import com.nutalig.constant.RfqSupplierQuoteStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqSupplierQuoteDto {

    private String id;
    private String rfqId;
    private SupplierDto supplier;
    private String inquiryId;
    private Integer revisionNo;
    private RfqSupplierQuoteStatus status;
    private String remark;
    private List<RfqSupplierQuoteDetailDto> details;
    private List<RfqSupplierQuoteAdditionalCostDto> additionalCosts;
    private List<RfqSupplierQuotePackageDto> packages;
    private List<RfqSupplierQuoteLeadTimeDto> leadTimes;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
