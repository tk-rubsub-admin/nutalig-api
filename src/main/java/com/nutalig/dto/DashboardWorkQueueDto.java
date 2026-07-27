package com.nutalig.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardWorkQueueDto {
    private String id;
    private String title;
    private String subtitle;
    private Long count;
    private String href;
    private List<DashboardQueueItemDto> items;
    private List<String> visibleTo;
}
