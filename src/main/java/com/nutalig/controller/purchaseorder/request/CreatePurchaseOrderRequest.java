package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePurchaseOrderRequest {
    private String salesOrderNo;
    private String supplierId;
    private Long supplierShippingId;
    private LocalDate docDate;
    private Integer productionLeadTimeDay;
    private Integer shippingLeadTimeDay;
    private String remark;
}
