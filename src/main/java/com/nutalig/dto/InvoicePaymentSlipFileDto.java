package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class InvoicePaymentSlipFileDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private Integer sortOrder;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
