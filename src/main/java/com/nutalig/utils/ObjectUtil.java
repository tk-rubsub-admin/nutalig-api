package com.nutalig.utils;

import com.nutalig.entity.SystemConfigEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectUtil {

    public static boolean isChanged(Object oldVal, Object newVal) {
        if (oldVal == null && newVal == null) return false;
        if (oldVal == null || newVal == null) return true;
        return !oldVal.equals(newVal);
    }

    public static <T> T changed(T oldVal, T newVal) {
        return isChanged(oldVal, newVal) ? newVal : oldVal;
    }

    public static SystemConfigEntity changedConfig(
            SystemConfigEntity oldVal,
            String newCode,
            Supplier<SystemConfigEntity> resolver
    ) {
        String oldCode = oldVal != null ? oldVal.getId().getCode() : null;

        if (isChanged(oldCode, newCode)) {
            return newCode != null ? resolver.get() : null;
        }

        return oldVal;
    }

    public static String extractJsonObject(String raw) {
        String s = raw.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s; // fallback
    }

}
