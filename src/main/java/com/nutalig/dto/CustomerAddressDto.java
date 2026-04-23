package com.nutalig.dto;

import com.nutalig.constant.AddressType;
import lombok.Data;

@Data
public class CustomerAddressDto {
    private String id;
    private AddressType addressType;
    private Boolean isDefault;
    private String label;
    private String fullAddress;
    private String addressLine1;
    private String addressLine2;
    private String subdistrict;
    private String district;
    private String province;
    private String postcode;
    private String country;
}
