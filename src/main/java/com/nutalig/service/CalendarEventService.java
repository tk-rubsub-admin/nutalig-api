package com.nutalig.service;

import com.nutalig.constant.CalendarEventStyleRegistry;
import com.nutalig.constant.CalendarEventType;
import com.nutalig.controller.user.request.CreateMyCalendarEventRequest;
import com.nutalig.controller.user.request.UpdateMyCalendarEventRequest;
import com.nutalig.controller.request.DateTimeRangeModelRequest;
import com.nutalig.dto.CalendarEventDto;
import com.nutalig.entity.CalendarEventEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.CalendarEventRepository;
import com.nutalig.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendarEvents(DateTimeRangeModelRequest request) {
        List<CalendarEventEntity> events;
        if (request != null && request.getStart() != null && request.getEnd() != null) {
            events = calendarEventRepository.findAllByActiveTrueAndEventTypeNotAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
                    CalendarEventType.PRIVATE,
                    request.getEnd(),
                    request.getStart()
            );
        } else {
            events = calendarEventRepository.findAllByActiveTrueAndEventTypeNotOrderByStartAtAsc(
                    CalendarEventType.PRIVATE
            );
        }

        return events.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getMyPrivateCalendarEvents(String userId, DateTimeRangeModelRequest request) {
        List<CalendarEventEntity> events;
        if (request != null && request.getStart() != null && request.getEnd() != null) {
            events = calendarEventRepository
                    .findAllByActiveTrueAndEventTypeAndCreatedByAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
                            CalendarEventType.PRIVATE,
                            userId,
                            request.getEnd(),
                            request.getStart()
                    );
        } else {
            events = calendarEventRepository.findAllByActiveTrueAndEventTypeAndCreatedByOrderByStartAtAsc(
                    CalendarEventType.PRIVATE,
                    userId
            );
        }

        return events.stream().map(this::toDto).toList();
    }

    @Transactional
    public CalendarEventDto createMyCalendarEvent(String userId, CreateMyCalendarEventRequest request)
            throws InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getTitle())) {
            throw new InvalidRequestException("Event title is required.");
        }
        if (request.getStart() == null || request.getEnd() == null) {
            throw new InvalidRequestException("Event start and end are required.");
        }
        if (request.getEnd().isBefore(request.getStart())) {
            throw new InvalidRequestException("Event end must be after or equal to start.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        CalendarEventType eventType = request.getEventType() != null
                ? request.getEventType()
                : CalendarEventType.PRIVATE;

        CalendarEventEntity entity = new CalendarEventEntity();
        entity.setTitle(StringUtils.trimToNull(request.getTitle()));
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setEventType(eventType);
        entity.setStartAt(request.getStart());
        entity.setEndAt(request.getEnd());
        entity.setAllDay(Boolean.TRUE.equals(request.getAllDay()));
        entity.setColorCode(CalendarEventStyleRegistry.resolveColorCode(eventType));
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setActive(Boolean.TRUE);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setCreatedDate(now);
        entity.setUpdatedDate(now);

        return toDto(calendarEventRepository.save(entity));
    }

    @Transactional
    public CalendarEventDto updateMyPrivateCalendarEvent(
            String userId,
            Long eventId,
            UpdateMyCalendarEventRequest request
    ) throws InvalidRequestException, DataNotFoundException {
        if (request == null || StringUtils.isBlank(request.getTitle())) {
            throw new InvalidRequestException("Event title is required.");
        }
        if (request.getStart() == null || request.getEnd() == null) {
            throw new InvalidRequestException("Event start and end are required.");
        }
        if (request.getEnd().isBefore(request.getStart())) {
            throw new InvalidRequestException("Event end must be after or equal to start.");
        }

        CalendarEventEntity entity = calendarEventRepository
                .findByIdAndActiveTrueAndEventTypeAndCreatedBy(eventId, CalendarEventType.PRIVATE, userId)
                .orElseThrow(() -> new DataNotFoundException("Calendar event not found"));

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        entity.setTitle(StringUtils.trimToNull(request.getTitle()));
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setStartAt(request.getStart());
        entity.setEndAt(request.getEnd());
        entity.setAllDay(Boolean.TRUE.equals(request.getAllDay()));
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setUpdatedBy(userId);
        entity.setUpdatedDate(now);

        return toDto(calendarEventRepository.save(entity));
    }

    @Transactional
    public void deleteMyPrivateCalendarEvent(String userId, Long eventId) throws DataNotFoundException {
        CalendarEventEntity entity = calendarEventRepository
                .findByIdAndActiveTrueAndEventTypeAndCreatedBy(eventId, CalendarEventType.PRIVATE, userId)
                .orElseThrow(() -> new DataNotFoundException("Calendar event not found"));

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        entity.setActive(Boolean.FALSE);
        entity.setUpdatedBy(userId);
        entity.setUpdatedDate(now);

        calendarEventRepository.save(entity);
    }

    private CalendarEventDto toDto(CalendarEventEntity entity) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setEventType(entity.getEventType());
        dto.setStatus(entity.getActive() != null && entity.getActive() ? "ACTIVE" : "INACTIVE");
        dto.setStart(entity.getStartAt());
        dto.setEnd(entity.getEndAt());
        dto.setAllDay(entity.getAllDay());
        dto.setColorCode(StringUtils.defaultIfBlank(
                entity.getColorCode(),
                CalendarEventStyleRegistry.resolveColorCode(entity.getEventType())
        ));
        dto.setRemark(entity.getRemark());
        dto.setActive(entity.getActive());
        return dto;
    }
}
