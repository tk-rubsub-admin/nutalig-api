package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderAttachmentDocumentType;
import lombok.Data;

@Data
public class PurchaseOrderAttachmentDto {
    private Long id;
    private String purchaseOrderNo;
    private Long purchaseOrderPaymentId;
    private PurchaseOrderAttachmentDocumentType documentType;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private String remark;
    private Integer sortOrder;
}
