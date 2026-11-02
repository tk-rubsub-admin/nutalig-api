package com.nutalig.dto;
import com.nutalig.constant.ShippingContainerStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
@Data
public class ShippingContainerDto {
    private Long id; private Long version; private String containerJobNo; private String containerNo; private String sealNo; private String trackingNo;
    private String containerSize; private String shippingMethod; private Long supplierShippingId; private String carrierName;
    private Long destinationId; private String destinationName; private String origin;
    private LocalDate plannedDepartureDate; private LocalDate plannedArrivalDate;
    private ZonedDateTime sealedAt; private ZonedDateTime departedAt; private ZonedDateTime receivedAt;
    private ShippingContainerStatus status; private BigDecimal capacityCbm; private BigDecimal maxCargoWeightKg;
    private BigDecimal totalCartons; private BigDecimal totalCbm; private BigDecimal totalNetWeightKg; private BigDecimal totalGrossWeightKg;
    private String remark; private String createdBy; private String updatedBy;
    private ZonedDateTime createdDate; private ZonedDateTime updatedDate;
    private List<Load> loads;
    @Data public static class Content {
        private Long purchaseOrderDetailId; private Long componentId;
        private String productName; private String componentName; private BigDecimal quantity;
    }
    @Data public static class Load {
        private Long id; private Long deliveryId; private String deliveryNo; private String purchaseOrderNo;
        private Long deliveryDetailId; private Long scrapBoxId; private String componentName;
        private Boolean active; private Boolean uniformPackagingConfirmed; private BigDecimal cartonCount; private BigDecimal quantity;
        private BigDecimal cbmM3; private BigDecimal netWeightKg; private BigDecimal grossWeightKg;
        private BigDecimal lengthCm; private BigDecimal widthCm; private BigDecimal heightCm;
        private List<Content> contents;
    }
    @Data public static class Source {
        private Long deliveryId; private Long deliveryDetailId; private Long scrapBoxId; private String name;
        private BigDecimal totalCartons; private BigDecimal availableCartons; private BigDecimal quantity;
        private BigDecimal cbmM3; private BigDecimal netWeightKg; private BigDecimal grossWeightKg;
        private Boolean canSplit; private List<Content> contents;
    }
}
