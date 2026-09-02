package com.nutalig.controller.rfq.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRequestPriceDetailRequest {

    private String optionName;
    private String plan;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String internalRemark;
    private String recommend;
    private BigDecimal commission;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private String supplierId;
    private List<CreateRequestPriceTierRequest> tiers;
    private List<CreateRequestPriceTierSplitRequest> tierSplits;

    @Data
    public static class CreateRequestPriceTierRequest {
        private BigDecimal quantity;
        private BigDecimal productPrice;
        private BigDecimal commission;
        private Currency currency;
        private String containerSize;
        private BigDecimal landFreightCost;
        private BigDecimal seaFreightCost;
        private Boolean isFcl;
        private Boolean isShareFCL;
        private BigDecimal landTotalPrice;
        private BigDecimal seaTotalPrice;
        private Long supplierQuoteTierId;
        private Integer sortOrder;
    }

    @Data
    public static class CreateRequestPriceTierSplitRequest {
        private String supplierId;
        private BigDecimal quantity;
        private BigDecimal sellPrice;
        private BigDecimal commission;
        private Currency currency;
        private String containerSize;
        private BigDecimal landFreightCost;
        private BigDecimal landFreightQty;
        private BigDecimal seaFreightQty;
        private BigDecimal seaFreightCost;
        private Boolean isFcl;
        private Boolean isShareFCL;
    }


}
