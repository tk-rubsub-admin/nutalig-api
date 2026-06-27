package com.nutalig.controller.customer.request;

import lombok.Data;


@Data
public class SearchCustomerRequest {

    private String idEqual;
    private String nameContain;
    private String typeEqual;
    private String tierEqual;
    private String segmentEqual;
    private String saleAccountEqual;
    private String keyword;
}
