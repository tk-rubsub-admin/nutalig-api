package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationRequestDto {
    private LocalDate docDate;
    private LocalDate effectiveDate;
    private String customerId;
    private String customerAddressId;
    private String customerContactId;
    private String salesId;
    private String coSaleId;
    private String remark;
    private BigDecimal discount;
    private BigDecimal freight;
    private Boolean isVat;
    private List<QuotationItemRequestDto> items;
}
