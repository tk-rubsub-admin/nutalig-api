package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdatePurchaseOrderRequest {
    private LocalDate docDate;
    private Integer productionLeadTimeDay;
    private Integer shippingLeadTimeDay;
    private String remark;
    private List<UpdatePurchaseOrderDetailRequest> items;
}
