package com.nutalig.dto;

import com.nutalig.constant.QuotationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class RfqQuotationDto {
    private String quotationNo;
    private String rfqId;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private QuotationStatus status;
    private DocumentStatusProfileDto statusProfile;
    private Integer revNo;
    private BigDecimal grandTotal;
    private String docDate;
    private Boolean isLatest;
}
