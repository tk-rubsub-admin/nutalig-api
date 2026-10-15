package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class PurchaseOrderDeliveryDto {
    private Long id;
    private String deliveryNo;
    private String purchaseOrderNo;
    private ZonedDateTime shippedAt;
    private LocalDate expectedWarehouseDate;
    private ZonedDateTime receivedAt;
    private String shippingMethod;
    private Long shippingLocationId;
    private String shippingLocationName;
    private PurchaseOrderDeliveryStatus status;
    private String remark;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private List<PurchaseOrderDeliveryDetailDto> details;
}
