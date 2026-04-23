package com.nutalig.dto.document;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuotationDocumentDto extends DefaultDocumentDto {

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
    private String custMobileNo;

    // Sales Account
    private String coSalesId;
    private String salesId;
    private String salesName;
    private String salesNickname;
    private String salesMobileNo;

    // Items
    private List<QuotationItemDocumentDto> items;
}
