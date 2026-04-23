package com.nutalig.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilTest {

    @Test
    void getTimeZone_shouldReturnCorrectData() {
        // When
        final ZoneId timeZone = DateUtil.getTimeZone();

        // Then
        assertEquals(ZoneId.of("Asia/Bangkok"), timeZone);
    }

    @Test
    void toLocalDate_shouldReturnCorrectData() {
        // When
        final ZonedDateTime utc = ZonedDateTime.of(LocalDate.of(2022, 10, 15).atTime(23, 59), ZoneId.of("UTC"));
        final LocalDate localDate = DateUtil.toLocalDate(utc);

        // Then
        assertEquals(LocalDate.of(2022, 10, 16), localDate);
    }

    @Test
    void toLocalTime_shouldReturnCorrectData() {
        // When
        final ZonedDateTime utc = ZonedDateTime.of(LocalDate.of(2022, 10, 15).atTime(23, 59), ZoneId.of("UTC"));
        final LocalTime localTime = DateUtil.toLocalTime(utc);

        // Then
        assertEquals(LocalTime.of(6, 59), localTime);
    }

    @Test
    void convertToDateViaInstant_shouldReturnCorrectData() {
        // When
        final LocalDate localDate = LocalDate.of(2022, 10, 15);
        final Date date = DateUtil.convertToDateViaInstant(localDate);

        // Then
        assertEquals(Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()), date);
    }

}