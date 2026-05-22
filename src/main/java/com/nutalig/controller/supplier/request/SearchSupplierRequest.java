package com.nutalig.controller.supplier.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class SearchSupplierRequest {

    private String idEqual;
    @JsonAlias("supplierNameContain")
    private String nameContain;
    @JsonAlias("supplierCodeContain")
    private String supplierCodeEqual;
    private String supplierEmailContain;
    private String statusEqual;
    private String countryCodeEqual;
    private String contactNameContain;
    private String contactNumberContain;
    private String productFamilyCodeEqual;
    private String productMaterialCodeEqual;
    private String keyword;
}
