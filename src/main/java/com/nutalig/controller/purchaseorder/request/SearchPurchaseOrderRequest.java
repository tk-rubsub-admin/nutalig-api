package com.nutalig.controller.purchaseorder.request;

import com.nutalig.constant.PurchaseOrderStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SearchPurchaseOrderRequest {
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String supplierId;
    private LocalDate docDateStart;
    private LocalDate docDateEnd;
    private PurchaseOrderStatus status;
    private List<PurchaseOrderStatus> statuses;
    private String keyword;
}
