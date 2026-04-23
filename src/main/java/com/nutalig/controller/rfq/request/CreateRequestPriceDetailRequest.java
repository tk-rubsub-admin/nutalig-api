package com.nutalig.controller.rfq.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRequestPriceDetailRequest {

    private String optionName;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private List<CreateRequestPriceTierRequest> tiers;

    @Data
    public static class CreateRequestPriceTierRequest {
        private BigDecimal quantity;
        private BigDecimal productPrice;
        private BigDecimal landFreightCost;
        private BigDecimal seaFreightCost;
        private BigDecimal landTotalPrice;
        private BigDecimal seaTotalPrice;
        private Integer sortOrder;
    }
}
