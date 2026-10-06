package com.nutalig.utils;

import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import com.nutalig.entity.SupplierShippingEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShippingMethodUtil {

    public static ShippingMethod getShippingMethodCategory(String shippingMethod) {
        String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(shippingMethod), Locale.ROOT);
        try {
            return switch (ShippingMethod.valueOf(normalized)) {
                case SEA_FCL_20GP, SEA_FCL_40HQ, SEA_SHARE_FCL_20GP, SEA_SHARE_FCL_40HQ -> ShippingMethod.SEA;
                default -> ShippingMethod.valueOf(normalized);
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ShippingMode getShippingMode(String shippingMethod) {
        String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(shippingMethod), Locale.ROOT);
        return normalized.startsWith("SEA_FCL_") || normalized.startsWith("SEA_SHARE_FCL_")
                ? ShippingMode.FCL : ShippingMode.STANDARD;
    }

    public static boolean matchesSupplierShipping(String shippingMethod, SupplierShippingEntity shipping) {
        ShippingMethod category = getShippingMethodCategory(shippingMethod);
        return shipping != null && category != null
                && category == shipping.getShippingMethod()
                && getShippingMode(shippingMethod) == (shipping.getShippingMode() == null
                ? ShippingMode.STANDARD : shipping.getShippingMode());
    }

    public static String getShippingMethodLabel(String shippingMethod) {
        return getShippingMethodLabel(shippingMethod, "-", false, false);
    }

    public static String getShippingCode(
            String shippingMethod,
            List<SupplierShippingEntity> shippingEntities
    ) {
        String normalizedShippingMethod = StringUtils.upperCase(StringUtils.trimToEmpty(shippingMethod), Locale.ROOT);
        if ("AIR".equals(normalizedShippingMethod)) {
            return "ขนส่งทางเครื่องบิน";
        }

        List<String> requestedMethods = "ALL".equals(normalizedShippingMethod)
                ? List.of("LAND", "SEA") : List.of(normalizedShippingMethod);
        List<SupplierShippingEntity> available = shippingEntities == null ? List.of() : shippingEntities;
        return String.join(" & ", requestedMethods.stream()
                .map(method -> available.stream()
                        .filter(shipping -> matchesSupplierShipping(method, shipping))
                        .map(SupplierShippingEntity::getCarCode)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .findFirst().orElse(""))
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
