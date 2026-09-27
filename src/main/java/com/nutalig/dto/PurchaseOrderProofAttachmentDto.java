package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderProofMediaType;
import lombok.Data;

@Data
public class PurchaseOrderProofAttachmentDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private PurchaseOrderProofMediaType mediaType;
    private String thumbnailUrl;
    private Integer sortOrder;
}
