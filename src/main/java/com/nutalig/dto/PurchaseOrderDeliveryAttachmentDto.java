package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderDeliveryAttachmentSection;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class PurchaseOrderDeliveryAttachmentDto {
    private Long id;
    private Long deliveryId;
    private String fileName;
    private String fileUrl;
    private PurchaseOrderDeliveryAttachmentSection deliverySection;
    private String uploadedBy;
    private ZonedDateTime uploadedAt;
}
