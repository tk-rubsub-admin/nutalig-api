package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class DashboardDataDto {
    private String range;
    private String dateFrom;
    private String dateTo;
    private ZonedDateTime generatedAt;
    private String source;
    private List<DashboardMetricDto> metrics;
    private List<DashboardTrendChartDto> trendCharts;
    private List<DashboardDistributionChartDto> distributionCharts;
    private List<DashboardWorkQueueDto> workQueues;
    private List<DashboardQuickLinkDto> quickLinks;
}
