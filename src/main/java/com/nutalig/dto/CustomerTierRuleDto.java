package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerTierRuleDto {
    private String tierCode;
    private String nameTh;
    private String nameEn;
    private Integer sort;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumOrderAmount;
}
