package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemRequestDto {
    private String id;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private String imageUrl;
}
