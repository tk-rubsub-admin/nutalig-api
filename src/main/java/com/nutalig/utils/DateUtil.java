package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtil {

    public static final DateTimeFormatter DD_MMM_YY = DateTimeFormatter.ofPattern("dd MMM yy");
    public static final DateTimeFormatter DD_MM_YY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DD_MM_YY_2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter DD_M_YY = DateTimeFormatter.ofPattern("dd/M/yyyy");
    public static final SimpleDateFormat YYYY_MM = new SimpleDateFormat("yyyyMM");
    public static final DateTimeFormatter YYYY_MM_2 = DateTimeFormatter.ofPattern("yyyyMM");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSZ = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final DateTimeFormatter DD_MMM_YY_THAI = DateTimeFormatter.ofPattern("dd MMM yyyy", new java.util.Locale("th", "TH"));
    public static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final LocalTime MIN_TIME = LocalTime.of(0, 0, 0, 0);
    public static final LocalTime MAX_TIME = LocalTime.of(23, 59, 59, 999999000);

    public static ZoneId getTimeZone() {
        return ZoneId.of("Asia/Bangkok");
    }

    public static LocalDate toLocalDate(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(getTimeZone()).toLocalDate();
    }

    public static LocalTime toLocalTime(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(getTimeZone()).toLocalTime();
    }

    public static Date convertToDateViaInstant(LocalDate dateToConvert) {
        return Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

}
