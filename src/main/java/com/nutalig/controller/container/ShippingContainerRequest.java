package com.nutalig.controller.container;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class ShippingContainerRequest {
    @Valid @Size(max=1000) private List<@NotNull Load> loads;
    private Long version;
    @Size(max=50) private String containerNo;
    @Size(max=100) private String sealNo;
    @Size(max=50) private String trackingNo;
    private String containerSize;
    private Long supplierShippingId;
    private Long destinationId;
    @Size(max=255) private String origin;
    private LocalDate plannedDepartureDate;
    private LocalDate plannedArrivalDate;
    @Digits(integer=12,fraction=6) private BigDecimal capacityCbm;
    @Digits(integer=12,fraction=6) private BigDecimal maxCargoWeightKg;
    @Size(max=2000) private String remark;
    @Data public static class Load {
        private Long version;
        private Long deliveryId;
        private Long deliveryDetailId;
        private Long scrapBoxId;
        @Digits(integer=12,fraction=0) private BigDecimal cartonCount;
        private Boolean confirmUniformPackaging;
    }
    @Data public static class Action {
        private Long version;
        @Size(max=2000) private String reason;
    }
}
