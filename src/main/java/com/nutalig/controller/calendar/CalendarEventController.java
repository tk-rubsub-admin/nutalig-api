package com.nutalig.controller.calendar;

import com.nutalig.controller.request.DateTimeRangeModelRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.CalendarEventDto;
import com.nutalig.service.CalendarEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/calendar-events")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping
    public GeneralResponse<List<CalendarEventDto>> getCalendarEvents(
            @ModelAttribute DateTimeRangeModelRequest request
    ) {
        log.info("=== Start get calendar events start {} end {} ===", request.getStart(), request.getEnd());

        List<CalendarEventDto> response = calendarEventService.getCalendarEvents(request);

        log.info("=== End get calendar events size {} ===", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }
}
