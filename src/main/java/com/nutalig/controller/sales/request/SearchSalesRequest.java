package com.nutalig.controller.sales.request;

import lombok.Data;

@Data
public class SearchSalesRequest {

    private String salesIdEqual;
    private String typeEqual;
    private String keyword;
}
