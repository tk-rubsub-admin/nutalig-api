package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.DeliveryContainerStatus;
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
    private String trackingNumber;
    private LocalDate inTransitDate;
    private Long shippingLocationId;
    private String shippingLocationName;
    private PurchaseOrderDeliveryStatus status;
    private DeliveryContainerStatus containerStatus;
    private List<ContainerReference> containers;

    @Data
    public static class ContainerReference {
        private Long id;
        private String containerJobNo;
        private String containerNo;
    }

    private String remark;
    private String createdBy;
    private String updatedBy;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private List<PurchaseOrderDeliveryDetailDto> details;
    private List<PurchaseOrderDeliveryScrapBoxDto> scrapBoxes;
}
