package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShippingMethodUtil {

    public static String getShippingMethodLabel(String shippingMethod) {
        return getShippingMethodLabel(shippingMethod, "-", false, false);
    }

    public static String getShippingMethodLabel(
            String shippingMethod,
            String fallback,
            boolean isFcl,
            boolean isShareFcl
    ) {
        if (shippingMethod == null || shippingMethod.isBlank()) {
            return fallback;
        }

        String normalized = shippingMethod.trim().toUpperCase(Locale.ROOT);
        if ("SEA".equals(normalized) && isShareFcl) {
            return "ขนส่งทางเรือ ปิดตู้แบบแชร์";
        }
        if ("SEA".equals(normalized) && isFcl) {
            return "ขนส่งทางเรือ ปิดตู้";
        }
        if (normalized.startsWith("SEA_SHARE_FCL_")) {
            return "ขนส่งทางเรือ ปิดตู้ "
                    + normalized.substring("SEA_SHARE_FCL_".length())
                    + " แบบแชร์";
        }
        if (normalized.startsWith("SEA_FCL_")) {
            return "ขนส่งทางเรือ ปิดตู้ "
                    + normalized.substring("SEA_FCL_".length());
        }

        return switch (normalized) {
            case "ALL" -> "ขนส่งทางรถ / ขนส่งทางเรือ";
            case "LAND" -> "ขนส่งทางรถ";
            case "SEA" -> "ขนส่งทางเรือ";
            case "AIR" -> "ขนส่งทางเครื่องบิน";
            default -> shippingMethod;
        };
    }
}
