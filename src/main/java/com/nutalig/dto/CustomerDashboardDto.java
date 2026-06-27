package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class CustomerDashboardDto {

    private ZonedDateTime generatedAt;
    private Long totalCustomers;
    private Long companyCustomers;
    private Long individualCustomers;
    private Long defaultAddressCustomers;
    private List<CustomerDashboardBreakdownDto> typeBreakdown;
    private List<CustomerDashboardBreakdownDto> tierBreakdown;
    private List<CustomerDashboardBreakdownDto> segmentBreakdown;
    private List<CustomerDto> recentCustomers;
}
