package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompletePurchaseOrderProductionRequest {
    private String note;
    private PurchaseOrderDelivery delivery;

    @Data
    public static class PurchaseOrderDelivery {
        private LocalDate shippedDate;
        private LocalDate expectedWarehouseDate;
        private LocalDate receivedDate;
        private String shippingMethod;
        private Long shippingLocationId;
        private List<Detail> details;
    }

    @Data
    public static class Detail {
        private Long purchaseOrderDetailId;
        private Long purchaseOrderDetailComponentId;
        private BigDecimal quantity;
        private BigDecimal cartonCount;
        private BigDecimal piecesPerCarton;
        private BigDecimal netWeightKg;
        private BigDecimal grossWeightKg;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal cbmM3;
        private Integer sortOrder;
    }
}
