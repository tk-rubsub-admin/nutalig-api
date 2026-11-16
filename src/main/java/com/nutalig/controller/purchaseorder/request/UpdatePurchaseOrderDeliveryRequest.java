package com.nutalig.controller.purchaseorder.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdatePurchaseOrderDeliveryRequest {
    private LocalDate expectedWarehouseDate;
    private String shippingMethod;
    private Long shippingLocationId;
    private List<Detail> details;
    private List<ScrapBox> scrapBoxes;

    @Data
    public static class Detail {
        private Long id;
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

    @Data
    public static class ScrapBox {
        private Long id;
        private BigDecimal netWeightKg;
        private BigDecimal grossWeightKg;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal cbmM3;
        private Integer sortOrder;
    }
}
