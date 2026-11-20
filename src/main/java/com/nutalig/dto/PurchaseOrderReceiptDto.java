package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderReceiptStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class PurchaseOrderReceiptDto {
    private String receiptNo;
    private String purchaseOrderNo;
    private Long deliveryId;
    private String deliveryNo;
    private String supplierName;
    private String shippingMethod;
    private String trackingNumber;
    private String shippingRouteCode;
    private String originLocationName;
    private String destinationLocationName;
    private Long warehouseId;
    private LocalDate receivedDate;
    private String receivedBy;
    private PurchaseOrderReceiptStatus status;
    private String remark;
    private ZonedDateTime createdDate;
    private List<PurchaseOrderReceiptDetailDto> details;
}
