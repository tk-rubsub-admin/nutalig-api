package com.nutalig.dto;

import com.nutalig.constant.QuotationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuotationDto {
    private String quotationNo;
    private String rfqId;
    private String referenceRfqId;
    private RfqReferenceDto referenceRfq;
    private String docDate;
    private String effectiveDate;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private QuotationCustomerSnapshotDto customerSnapshot;
    private EmployeeDto saleAccount;
    private String coSaleId;
    private QuotationStatus status;
    private DocumentStatusProfileDto statusProfile;
    private String remark;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal subTotal;
    private BigDecimal vat;
    private BigDecimal vatRate;
    private BigDecimal grandTotal;
    private List<QuotationItemRequestDto> items;
    private Integer revNo;
    private Boolean isShowSummary;
    private String shipping;
}
