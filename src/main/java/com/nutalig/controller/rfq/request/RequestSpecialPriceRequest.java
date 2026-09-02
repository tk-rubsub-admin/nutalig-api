package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RequestSpecialPriceRequest {
    private List<TierTargetPriceRequest> tiers;

    @Data
    public static class TierTargetPriceRequest {
        private Long tierId;
        private BigDecimal targetPrice;
    }
}
