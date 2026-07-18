package com.nutalig.controller.freelancesale.request;

import lombok.Data;

@Data
public class CreateFreelanceSaleRequest {
    private String id;
    private String name;
    private String contactNumber;
    private String saleCoverage;
    private String additional;
}
