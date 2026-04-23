package com.nutalig.controller.customer.request;

import lombok.Data;


@Data
public class SearchCustomerRequest {

    private String idEqual;
    private String nameContain;
    private String typeEqual;
    private String saleAccountEqual;
    private String keyword;
}
