package com.nutalig.controller.customer.request;

import com.nutalig.constant.AddressType;
import lombok.Data;

@Data
public class CreateCustomerAddressRequest {
    private AddressType addressType;
    private Boolean isDefault;
    private String label;
    private String addressLine1;
    private String addressLine2;
    private String subdistrict;
    private String district;
    private String province;
    private String postcode;
    private String country;
}
