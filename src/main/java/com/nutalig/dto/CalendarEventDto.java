package com.nutalig.dto;

import com.nutalig.constant.CalendarEventType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CalendarEventDto {
    private Long id;
    private String title;
    private String description;
    private CalendarEventType eventType;
    private String status;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private Boolean allDay;
    private String colorCode;
    private String remark;
    private Boolean active;
}
