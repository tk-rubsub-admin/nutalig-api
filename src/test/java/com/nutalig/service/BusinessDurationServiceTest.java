package com.nutalig.service;

import com.nutalig.repository.CalendarEventRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class BusinessDurationServiceTest {
    @Mock CalendarEventRepository calendarRepository;
    @InjectMocks BusinessDurationService service;

    @ParameterizedTest
    @CsvSource({
            "2026-10-05T16:00:00+07:00, 2026-10-06T10:00:00+07:00, 17:00, 09:00, 1080",
            "2026-10-05T17:00:00+07:00, 2026-10-06T10:00:00+07:00, 17:00, 09:00, 1020",
            "2026-10-05T17:30:00+07:00, 2026-10-06T10:00:00+07:00, 17:00, 09:00, 60",
            "2026-10-05T17:30:00+07:00, 2026-10-06T08:00:00+07:00, 17:00, 09:00, 0",
            "2026-10-05T17:30:00+07:00, 2026-10-06T09:00:00+07:00, 17:00, 09:00, 0",
            "2026-10-05T10:30:00Z, 2026-10-06T03:00:00Z, 17:00, 09:00, 60",
            "2026-10-05T15:30:00+07:00, 2026-10-06T11:00:00+07:00, 15:00, 10:30, 30",
            "2026-10-05T17:30:00+07:00, 2026-10-07T10:00:00+07:00, 17:00, 13:00, 1260",
            "2026-10-05T17:30:00+07:00, 2026-10-06T10:00:00+07:00, , 09:00, 990",
            "2026-10-05T17:30:00+07:00, 2026-10-06T10:00:00+07:00, 17:00, , 60"
    })
    void countsFromActualRequestOrConfiguredNextDayStart(
            String requestedAt, String endedAt, String cutoff, String nextDayStart, long expectedMinutes
    ) {
        long minutes = service.calculateBusinessDurationMinutesWithCutoff(
                ZonedDateTime.parse(requestedAt),
                ZonedDateTime.parse(endedAt),
                cutoff != null ? LocalTime.parse(cutoff) : null,
                nextDayStart != null ? LocalTime.parse(nextDayStart) : null
        );

        assertEquals(expectedMinutes, minutes);
    }
}
