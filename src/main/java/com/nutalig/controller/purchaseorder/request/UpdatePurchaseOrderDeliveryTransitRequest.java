package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePurchaseOrderDeliveryTransitRequest {
    private String trackingNumber;
    private LocalDate inTransitDate;
}
