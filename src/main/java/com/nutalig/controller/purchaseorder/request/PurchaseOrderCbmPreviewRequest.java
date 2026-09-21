package com.nutalig.controller.purchaseorder.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderCbmPreviewRequest {
    private String salesOrderNo;
    private List<Item> items;

    @Data
    public static class Item {
        private Long salesOrderDetailId;
        private BigDecimal quantity;
    }
}
