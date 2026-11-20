package com.nutalig.dto.document;

import lombok.Data;

import java.io.InputStream;

@Data
public class CartonLabelDocumentDto {
    private InputStream logo;
    private String purchaseNo;
    private String salesOrderNo;
    private String productName;
    private String spec;
    private String component;
    private String dimension;
    private String weight;
    private InputStream qrCode;
}
