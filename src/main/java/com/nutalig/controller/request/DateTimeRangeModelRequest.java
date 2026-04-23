package com.nutalig.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeRangeModelRequest {
    private ZonedDateTime start;
    private ZonedDateTime end;
}
