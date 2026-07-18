package com.nutalig.dto;

import lombok.Data;

@Data
public class SalesOrderAttachmentDto {
    private Long id;
    private String salesOrderNo;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private String remark;
    private Integer sortOrder;
}
