package com.nutalig.controller.request;

import com.nutalig.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeModelRequest {
    private ZonedDateTime start;
    private ZonedDateTime end;

    public ZonedDateTime getStart() {
        if (start == null) {
            return null;
        }
        return this.start.withZoneSameInstant(DateUtil.getTimeZone());
    }

    public ZonedDateTime getEnd() {
        if (end == null) {
            return null;
        }
        return this.end.withZoneSameInstant(DateUtil.getTimeZone());
    }
}
