package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePurchaseOrderMilestoneRequest {
    private LocalDate plannedDate;
    private String note;
}
