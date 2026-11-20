package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderReceiptRequest {
    private Long warehouseId;
    private LocalDate receivedDate;
    private String remark;
    private List<Detail> details;

    @Data
    public static class Detail {
        private Long deliveryDetailId;
        private BigDecimal receivedQuantity;
        private BigDecimal damagedQuantity;
        private BigDecimal shortageQuantity;
        private BigDecimal excessQuantity;
        private String lotNo;
        private LocalDate expiryDate;
        private Long warehouseLocationId;
        private String remark;
    }
}
