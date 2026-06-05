package com.nutalig.controller.rfq.request;

import com.nutalig.constant.RfqSupplierQuoteStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpsertRfqSupplierQuoteRequest {

    private String supplierId;
    private String inquiryId;
    private RfqSupplierQuoteStatus status;
    private String remark;
    private List<DetailRequest> details;
    private List<AdditionalCostRequest> additionalCosts;

    @Data
    public static class DetailRequest {
        private Long rfqDetailId;
        private String optionName;
        private String spec;
        private Integer sortOrder;
        private String remark;
        private List<TierRequest> tiers;
    }

    @Data
    public static class TierRequest {
        private BigDecimal quantity;
        private BigDecimal productPrice;
        private BigDecimal landFreightCost;
        private BigDecimal seaFreightCost;
        private BigDecimal landTotalPrice;
        private BigDecimal seaTotalPrice;
        private Integer sortOrder;
    }

    @Data
    public static class AdditionalCostRequest {
        private String description;
        private String unit;
        private String value;
        private Integer sortOrder;
    }
}
