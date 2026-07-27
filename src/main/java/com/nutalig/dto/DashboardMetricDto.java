package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardMetricDto {
    private String id;
    private String title;
    private String value;
    private String subtitle;
    private String trend;
    private String tone;
    private String href;
    private List<String> visibleTo;
}
