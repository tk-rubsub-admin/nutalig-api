package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestedMoqDto {
    private BigDecimal moq;
    private BigDecimal targetPrice;
}
