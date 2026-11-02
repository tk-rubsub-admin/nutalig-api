package com.nutalig.dto;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SupplierShippingRouteDto {
    private Long id;
    private Long supplierShippingId;
    private String supplierShippingName;
    private ShippingMethod shippingMethod;
    private ShippingMode shippingMode;
    private String carCode;
    private Long shippingRouteId;
    private String routeCode;
    private Long originLocationId;
    private String originLocationName;
    private Long destinationLocationId;
    private String destinationLocationName;
    private String currency;
    private BigDecimal baseCost;
    private Integer leadTimeDayMin;
    private Integer leadTimeDayMax;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String remark;
    private Boolean active;
}
