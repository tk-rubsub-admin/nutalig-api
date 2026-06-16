package com.nutalig.dto;

import com.nutalig.constant.RfqSupplierInquiryStatus;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqSupplierInquiryDto {

    private String id;
    private String rfqId;
    private Integer versionNo;
    private RfqSupplierInquiryStatus status;
    private String thaiMessage;
    private String chineseMessage;
    private String sourceSnapshot;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
