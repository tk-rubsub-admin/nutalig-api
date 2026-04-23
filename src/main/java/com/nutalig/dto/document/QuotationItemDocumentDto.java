package com.nutalig.dto.document;

import lombok.Data;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;

@Data
public class QuotationItemDocumentDto {
    private InputStream image;
    private Integer no;
    private String name;
    private String type;
    private String capacity;
    private String size;
    private String spec;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal amount;
}
