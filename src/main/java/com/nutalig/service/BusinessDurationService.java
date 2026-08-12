package com.nutalig.service;

import com.nutalig.constant.CalendarEventType;
import com.nutalig.entity.CalendarEventEntity;
import com.nutalig.repository.CalendarEventRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessDurationService {

    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public long calculateBusinessDurationMinutes(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0L;
        }

        ZonedDateTime start = startDateTime.withZoneSameInstant(DateUtil.getTimeZone());
        ZonedDateTime end = endDateTime.withZoneSameInstant(DateUtil.getTimeZone());
        if (end.isBefore(start) || end.isEqual(start)) {
            return 0L;
        }

        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        Set<LocalDate> holidays = resolveHolidayDates(startDate, endDate);

        long totalMinutes = 0L;
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            if (!isBusinessDay(currentDate) || holidays.contains(currentDate)) {
                continue;
            }

            ZonedDateTime dayStart = currentDate.atStartOfDay(DateUtil.getTimeZone());
            ZonedDateTime dayEnd = currentDate.plusDays(1).atStartOfDay(DateUtil.getTimeZone());
            ZonedDateTime overlapStart = start.isAfter(dayStart) ? start : dayStart;
            ZonedDateTime overlapEnd = end.isBefore(dayEnd) ? end : dayEnd;

            if (overlapEnd.isAfter(overlapStart)) {
                totalMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }

        return totalMinutes;
    }

    @Transactional(readOnly = true)
    public long calculateBusinessDurationMinutesWithCutoff(
            ZonedDateTime requestedDateTime,
            ZonedDateTime endDateTime,
            LocalTime cutoffTime
    ) {
        if (requestedDateTime == null || endDateTime == null) {
            return 0L;
        }

        ZonedDateTime adjustedStartDateTime = adjustStartDateTimeByCutoff(requestedDateTime, cutoffTime);
        return calculateBusinessDurationMinutes(adjustedStartDateTime, endDateTime);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateBusinessDurationHours(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        long minutes = calculateBusinessDurationMinutes(startDateTime, endDateTime);
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Set<LocalDate> resolveHolidayDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return Set.of();
        }

        ZonedDateTime rangeStart = startDate.atStartOfDay(DateUtil.getTimeZone());
        ZonedDateTime rangeEnd = endDate.atTime(DateUtil.MAX_TIME).atZone(DateUtil.getTimeZone());

        return calendarEventRepository
                .findAllByActiveTrueAndEventTypeAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
                        CalendarEventType.HOLIDAY,
                        rangeEnd,
                        rangeStart
                )
                .stream()
                .flatMap(event -> extractHolidayDates(event, startDate, endDate).stream())
                .collect(Collectors.toSet());
    }

    private Set<LocalDate> extractHolidayDates(CalendarEventEntity event, LocalDate startDate, LocalDate endDate) {
        if (event == null || event.getStartAt() == null || event.getEndAt() == null) {
            return Set.of();
        }

        LocalDate eventStart = event.getStartAt().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate();
        LocalDate eventEnd = event.getEndAt().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate();
        LocalDate from = eventStart.isAfter(startDate) ? eventStart : startDate;
        LocalDate to = eventEnd.isBefore(endDate) ? eventEnd : endDate;

        if (to.isBefore(from)) {
            return Set.of();
        }

        return from.datesUntil(to.plusDays(1)).collect(Collectors.toSet());
    }

    private boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY;
    }

    private ZonedDateTime adjustStartDateTimeByCutoff(ZonedDateTime requestedDateTime, LocalTime cutoffTime) {
        ZonedDateTime normalizedRequestedDateTime = requestedDateTime.withZoneSameInstant(DateUtil.getTimeZone());
        if (cutoffTime == null) {
            return normalizedRequestedDateTime;
        }

        if (normalizedRequestedDateTime.toLocalTime().isAfter(cutoffTime)) {
            return normalizedRequestedDateTime
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(DateUtil.getTimeZone());
        }

        return normalizedRequestedDateTime;
    }
}
