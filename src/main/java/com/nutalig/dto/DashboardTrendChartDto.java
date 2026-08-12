package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardTrendChartDto {
    private String id;
    private String title;
    private String subtitle;
    private String unit;
    private List<String> labels;
    private List<DashboardSeriesDto> series;
    private List<String> visibleTo;
}
