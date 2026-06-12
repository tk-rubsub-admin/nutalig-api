package com.nutalig.service;

import com.nutalig.controller.request.DateTimeRangeModelRequest;
import com.nutalig.dto.CalendarEventDto;
import com.nutalig.entity.CalendarEventEntity;
import com.nutalig.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendarEvents(DateTimeRangeModelRequest request) {
        List<CalendarEventEntity> events;
        if (request != null && request.getStart() != null && request.getEnd() != null) {
            events = calendarEventRepository.findAllByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
                    request.getEnd(),
                    request.getStart()
            );
        } else {
            events = calendarEventRepository.findAllByActiveTrueOrderByStartAtAsc();
        }

        return events.stream().map(this::toDto).toList();
    }

    private CalendarEventDto toDto(CalendarEventEntity entity) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setEventType(entity.getEventType());
        dto.setStart(entity.getStartAt());
        dto.setEnd(entity.getEndAt());
        dto.setAllDay(entity.getAllDay());
        dto.setColorCode(entity.getColorCode());
        dto.setRemark(entity.getRemark());
        dto.setActive(entity.getActive());
        return dto;
    }
}
