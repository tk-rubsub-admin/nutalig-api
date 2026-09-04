package com.nutalig.controller.purchaseorder.request;

import com.nutalig.constant.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderRequest {
    private String salesOrderNo;
    private String supplierId;
    private Long supplierShippingId;
    private LocalDate docDate;
    private Integer productionLeadTimeDay;
    private Integer shippingLeadTimeDay;
    private String paymentTerm;
    private String shippingMethodSnapshot;
    private String containerSizeSnapshot;
    private String supplierContactSnapshot;
    private String supplierContactNoSnapshot;
    private String supplierAddressSnapshot;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long salesOrderDetailId;
        private String name;
        private String spec;
        private BigDecimal quantity;
        private Currency supplierCurrency;
        private BigDecimal supplierUnitPrice;
        private BigDecimal supplierShippingCost;
    }
}
