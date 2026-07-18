package com.nutalig.constant;

import java.util.EnumMap;
import java.util.Map;

public final class CalendarEventStyleRegistry {

    private static final String DEFAULT_COLOR_CODE = "#64748b";

    private static final Map<CalendarEventType, String> COLOR_BY_TYPE =
            new EnumMap<>(CalendarEventType.class);

    static {
        COLOR_BY_TYPE.put(CalendarEventType.HOLIDAY, "#ef4444");
        COLOR_BY_TYPE.put(CalendarEventType.CHINA_HOLIDAY, "#dc2626");
        COLOR_BY_TYPE.put(CalendarEventType.PRIVATE, "#2563eb");
        COLOR_BY_TYPE.put(CalendarEventType.INTERNAL, "#7c3aed");
        COLOR_BY_TYPE.put(CalendarEventType.AIR_SHIPPING, "#0ea5e9");
        COLOR_BY_TYPE.put(CalendarEventType.SEA_SHIPPING, "#0891b2");
        COLOR_BY_TYPE.put(CalendarEventType.LAND_SHIPPING, "#16a34a");
    }

    private CalendarEventStyleRegistry() {
    }

    public static String resolveColorCode(CalendarEventType eventType) {
        if (eventType == null) {
            return DEFAULT_COLOR_CODE;
        }
        return COLOR_BY_TYPE.getOrDefault(eventType, DEFAULT_COLOR_CODE);
    }
}
