package com.nutalig.dto;

import com.nutalig.constant.QuotationStatus;
import jakarta.persistence.Column;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationDto {
    private String quotationNo;
    private String docDate;
    private String effectiveDate;
    private CustomerDto customer;
    private CustomerAddressDto customerAddress;
    private CustomerContactDto customerContact;
    private SalesAccountDto saleAccount;
    private String coSaleId;
    private QuotationStatus status;
    private String remark;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal subTotal;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private List<QuotationItemRequestDto> items;
}
