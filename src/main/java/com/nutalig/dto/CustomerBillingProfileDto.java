package com.nutalig.dto;

import com.nutalig.constant.BillingProfileType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CustomerBillingProfileDto {
    private Long id;
    private String customerId;
    private String profileName;
    private BillingProfileType billingProfileType;
    private String billingName;
    private String taxId;
    private String branchCode;
    private String branchName;
    private String addressLine1;
    private String addressLine2;
    private String subdistrict;
    private String district;
    private String province;
    private String postcode;
    private String country;
    private String email;
    private String phone;
    private Boolean isDefault;
    private Boolean active;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
