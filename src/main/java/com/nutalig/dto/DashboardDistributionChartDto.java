package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardDistributionChartDto {
    private String id;
    private String title;
    private String subtitle;
    private List<DashboardDistributionItemDto> items;
    private List<String> visibleTo;
}
