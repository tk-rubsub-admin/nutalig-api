package com.nutalig.dto.document;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class QuotationDocumentDto extends DefaultDocumentDto {

    private String currency;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private BigDecimal freight;
    private BigDecimal subTotal;
    private BigDecimal vat;
    private String remark;
    private String thaiBahtText;

    // Customer
    private String custName;
    private String custTaxId;
    private String custAddress;
    private String custContactName;
    private String custMobileNo;
    private String paymentTerm;

    // Sales Account
    private String coSalesId;
    private String salesId;
    private String salesName;
    private String salesNickname;
    private String salesMobileNo;

    private String project;
    private String orderType;
    private String shipping;
    private String shippingLabel;
    private String tolerance;
    private String productionLeadTime;
    private String sample;
    private String shippingLeadTime;
    private String moldLeadTime;

    // Items
    private List<QuotationItemDocumentDto> items;
}
