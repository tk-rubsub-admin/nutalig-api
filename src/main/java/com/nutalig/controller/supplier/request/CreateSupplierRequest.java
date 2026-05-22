package com.nutalig.controller.supplier.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateSupplierRequest {

    private String supplierName;
    private String supplierCode;
    private String supplierEmail;
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
    private List<CreateSupplierContactRequest> contacts;
}
