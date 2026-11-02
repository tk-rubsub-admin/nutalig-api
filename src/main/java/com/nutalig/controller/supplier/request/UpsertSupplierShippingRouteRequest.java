package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpsertSupplierShippingRouteRequest {
    private Long supplierShippingId;
    private Long shippingRouteId;
    private String currency;
    private BigDecimal baseCost;
    private Integer leadTimeDayMin;
    private Integer leadTimeDayMax;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String remark;
}
