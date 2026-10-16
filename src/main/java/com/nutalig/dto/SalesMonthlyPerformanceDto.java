package com.nutalig.dto;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesMonthlyPerformanceDto {
    private String salesId;
    private Integer year;
    private Integer month;
    private Currency currency;
    private BigDecimal depositAmount;
    private BigDecimal salesAmount;
    private BigDecimal interimInstallmentAmount;
}
