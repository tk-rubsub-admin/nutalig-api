package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSeriesDto {
    private String name;
    private List<Double> data;
    private String color;
}
