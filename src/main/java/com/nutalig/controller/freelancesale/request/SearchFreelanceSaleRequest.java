package com.nutalig.controller.freelancesale.request;

import lombok.Data;

@Data
public class SearchFreelanceSaleRequest {
    private String keyword;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
