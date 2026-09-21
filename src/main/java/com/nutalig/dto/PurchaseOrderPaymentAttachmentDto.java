package com.nutalig.dto;

import lombok.Data;

@Data
public class PurchaseOrderPaymentAttachmentDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private Integer sortOrder;
}
