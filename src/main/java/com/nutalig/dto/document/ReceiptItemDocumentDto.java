package com.nutalig.dto.document;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiptItemDocumentDto {
    private Integer no;
    private String sku;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal amount;
}
