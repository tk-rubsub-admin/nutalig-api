package com.nutalig.utils;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.entity.SupplierShippingEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShippingMethodUtil {

    public static String getShippingMethodLabel(String shippingMethod) {
        return getShippingMethodLabel(shippingMethod, "-", false, false);
    }

    public static String getShippingCode(
            String shippingMethod,
            List<SupplierShippingEntity> shippingEntities
    ) {
        String normalizedShippingMethod = StringUtils.upperCase(StringUtils.trimToEmpty(shippingMethod));
        if ("AIR".equals(normalizedShippingMethod)) {
            return "ขนส่งทางเครื่องบิน";
        }

        List<ShippingMethod> requestedMethods = switch (normalizedShippingMethod) {
            case "LAND" -> List.of(ShippingMethod.LAND);
            case "SEA" -> List.of(ShippingMethod.SEA);
            case "ALL" -> List.of(ShippingMethod.LAND, ShippingMethod.SEA);
            default -> List.of();
        };
        if (requestedMethods.isEmpty()) {
            return "";
        }

        Map<ShippingMethod, String> shippingCodes = new EnumMap<>(ShippingMethod.class);
        if (shippingEntities != null) {
            shippingEntities.forEach(shipping -> {
                if (shipping.getShippingMethod() != null && StringUtils.isNotBlank(shipping.getCarCode())) {
                    shippingCodes.putIfAbsent(shipping.getShippingMethod(), shipping.getCarCode().trim());
                }
            });
        }

        return String.join(" & ", requestedMethods.stream()
                .map(shippingCodes::get)
                .filter(StringUtils::isNotBlank)
                .toList());
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
