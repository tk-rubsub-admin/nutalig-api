package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierShippingDestinationDto {
    private Long id;
    private Long supplierShippingId;
    private String destinationCode;
    private String destinationName;
    private String countryCode;
    private String province;
    private String district;
    private String subdistrict;
    private String postalCode;
    private String fullAddress;
    private BigDecimal additionalCost;
    private Integer sortOrder;
}
