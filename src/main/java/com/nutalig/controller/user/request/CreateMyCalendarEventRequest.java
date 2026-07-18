package com.nutalig.controller.user.request;

import com.nutalig.constant.CalendarEventType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CreateMyCalendarEventRequest {
    private String title;
    private String description;
    private CalendarEventType eventType;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private Boolean allDay;
    private String remark;
}
