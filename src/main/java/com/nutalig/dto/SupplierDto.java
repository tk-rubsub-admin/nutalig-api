package com.nutalig.dto;

import com.nutalig.constant.Status;
import lombok.Data;

import java.util.List;

@Data
public class SupplierDto {

    private String id;
    private String supplierName;
    private String supplierCode;
    private String supplierEmail;
    private Status status;
    private String fullAddress;
    private String fullAddressEn;
    private String countryCode;
    private String province;
    private String city;
    private String district;
    private String town;
    private String street;
    private String detailAddress;
    private String postalCode;
    private String additional;
    private List<SupplierContactDto> contacts;
    private List<SupplierCapabilityDto> capabilities;

}
