package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.ShippingMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SupplierShippingDto {
    private Long id;
    private ShippingMethod shippingMethod;
    private String shippingName;
    private String originCountryCode;
    private String originProvince;
    private Currency currency;
    private BigDecimal baseCost;
    private Integer leadTimeDayMin;
    private Integer leadTimeDayMax;
    private String remark;
    private String carCode;
    private List<SupplierShippingDestinationDto> destinations;
}
