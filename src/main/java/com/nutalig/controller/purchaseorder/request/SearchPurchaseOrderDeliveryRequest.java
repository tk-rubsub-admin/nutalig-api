package com.nutalig.controller.purchaseorder.request;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.DeliveryContainerStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SearchPurchaseOrderDeliveryRequest {
    private String deliveryNo;
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String supplierId;
    private Long supplierShippingId;
    private Long shippingLocationId;
    private String supplierName;
    private String keyword;
    private String shippingMethod;
    private PurchaseOrderDeliveryStatus status;
    private DeliveryContainerStatus containerStatus;
    private List<DeliveryContainerStatus> containerStatuses;
    private LocalDate shippedDateStart;
    private LocalDate shippedDateEnd;
    private LocalDate expectedWarehouseDateStart;
    private LocalDate expectedWarehouseDateEnd;
}
