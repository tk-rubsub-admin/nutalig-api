package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardQuickLinkDto {
    private String id;
    private String title;
    private String description;
    private String href;
    private String icon;
    private List<String> visibleTo;
}
