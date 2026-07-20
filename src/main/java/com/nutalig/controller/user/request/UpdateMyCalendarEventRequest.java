package com.nutalig.controller.user.request;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class UpdateMyCalendarEventRequest {
    private String title;
    private String description;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private Boolean allDay;
    private String remark;
}
