package com.nutalig.controller.supplier.request;

import lombok.Data;

@Data
public class UpsertShippingLocationRequest {
    private String code;
    private String name;
    private String locationType;
    private String countryCode;
    private String province;
    private String district;
    private String address;
}
