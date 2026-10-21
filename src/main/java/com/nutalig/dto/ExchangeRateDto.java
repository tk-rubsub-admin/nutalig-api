package com.nutalig.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateDto {
    private String date;
    private String base;
    private String quote;
    private BigDecimal rate;
}
