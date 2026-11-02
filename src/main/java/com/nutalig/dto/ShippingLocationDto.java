package com.nutalig.dto;

import lombok.Data;

@Data
public class ShippingLocationDto {
    private Long id;
    private String code;
    private String name;
    private SystemConfigDto locationType;
    private String countryCode;
    private String province;
    private String district;
    private String address;
    private Boolean active;
}
