package com.nutalig.controller.salesorder.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateSalesOrderRequest {
    private LocalDate docDate;
    private LocalDate expireDate;
    private String coSaleId;
    private BigDecimal amount;
    private BigDecimal commission;
    private BigDecimal discount;
    private BigDecimal freight;
    private Boolean isVat;
    private String shippingType;
    private String remark;
    private List<UpdateSalesOrderDetailRequest> items;
}
