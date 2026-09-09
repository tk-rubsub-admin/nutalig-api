package com.nutalig.dto;

import com.nutalig.constant.QuotationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationRequestDto {
    private QuotationStatus status;
    private String rfqId;
    private List<String> rfqIds;
    private LocalDate docDate;
    private LocalDate effectiveDate;
    private String customerId;
    private String customerAddressId;
    private String customerContactId;
    private String customerBranchCode;
    private QuotationCustomerSnapshotDto customerSnapshot;
    private String salesId;
    private String coSaleId;
    private String remark;
    private BigDecimal discount;
    private BigDecimal freight;
    private Boolean isVat;
    private Boolean isShowSummary;
    private List<QuotationItemRequestDto> items;
    private String shipping;
    private String project;
    private String sampleLeadTime;
    private String productionLeadTime;
    private String shippingLeadTime;
    private String moldLeadTime;
    private String productQtyTolerance;
}
