package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThaiBahtText {

    private static final String[] NUMBER_TEXT = {
            "", "หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก", "เจ็ด", "แปด", "เก้า"
    };

    // ตำแหน่งภายใน "กลุ่ม 6 หลัก" (หน่วย -> แสน)
    private static final String[] POSITION_TEXT = {
            "", "สิบ", "ร้อย", "พัน", "หมื่น", "แสน"
    };

    public static String convertBahtText(BigDecimal amount) {
        if (amount == null) {
            return "ศูนย์บาทถ้วน";
        }

        // ปัดเป็น 2 ตำแหน่ง เพื่อสตางค์
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);

        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "ศูนย์บาทถ้วน";
        }

        boolean negative = normalized.signum() < 0;
        normalized = normalized.abs();

        String[] parts = normalized.toPlainString().split("\\.");
        String bahtPart = parts[0];
        String satangPart = (parts.length > 1) ? parts[1] : "00";
        if (satangPart.length() == 1) satangPart = satangPart + "0";
        if (satangPart.length() > 2) satangPart = satangPart.substring(0, 2);

        String bahtText = convertNumberToThaiText(bahtPart);

        StringBuilder result = new StringBuilder();
        if (negative) result.append("ลบ");

        result.append(bahtText).append("บาท");

        int satang = Integer.parseInt(satangPart);
        if (satang == 0) {
            result.append("ถ้วน");
        } else {
            result.append(convertNumberToThaiText(satangPart)).append("สตางค์");
        }

        return result.toString();
    }

    /**
     * แปลงเลขจำนวนเต็ม (string) เป็นคำอ่านไทย รองรับ "ล้าน" แบบเป็นกลุ่ม 6 หลัก
     */
    private static String convertNumberToThaiText(String number) {
        if (number == null || number.isBlank()) return "";
        number = number.replaceFirst("^0+(?!$)", ""); // ตัด leading zeros

        if ("0".equals(number)) {
            return "ศูนย์";
        }

        StringBuilder result = new StringBuilder();

        // แบ่งเป็นกลุ่มละ 6 หลักจากขวา: [..][..][xxxxxx]
        int len = number.length();
        int firstGroupLen = len % 6;
        if (firstGroupLen == 0) firstGroupLen = 6;

        int index = 0;
        int groupCount = 0;

        while (index < len) {
            int groupLen = (groupCount == 0) ? firstGroupLen : 6;
            String group = number.substring(index, index + groupLen);
            String groupText = convertUpTo6Digits(group);

            if (!groupText.isEmpty()) {
                if (result.length() > 0) result.append(""); // เผื่ออยากคั่น (ไม่ต้องคั่น)
                result.append(groupText);

                // ถ้ายังมีกลุ่มถัดไป (ด้านขวา) ให้เติม "ล้าน"
                if (index + groupLen < len) {
                    result.append("ล้าน");
                }
            } else {
                // ถ้ากลุ่มนี้เป็น 0 ทั้งกลุ่ม แต่ยังมีกลุ่มถัดไป
                // ต้องเติม "ล้าน" ไหม? โดยหลักภาษาไทย หากกลุ่มซ้ายเป็นศูนย์ทั้งกลุ่ม มักไม่อ่าน
                // ตัวอย่าง 1,000,000 = "หนึ่งล้าน" (กลุ่มซ้าย=1 กลุ่มขวา=000000 ไม่อ่าน)
                // ตัวอย่าง 1,000,000,000 = "หนึ่งพันล้าน" (กลุ่ม=001000 + 000000)
                // กรณีนี้ถ้ากลุ่มซ้ายว่าง เราจะไม่เติมอะไร
            }

            index += groupLen;
            groupCount++;
        }

        return result.toString();
    }

    /**
     * แปลงเลขภายในกลุ่ม 1-6 หลัก (หน่วยถึงแสน)
     */
    private static String convertUpTo6Digits(String group) {
        String g = group.replaceFirst("^0+(?!$)", ""); // ตัด leading zeros
        if (g.isEmpty() || "0".equals(g)) return "";

        int len = group.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < len; i++) {
            int digit = Character.getNumericValue(group.charAt(i));
            int pos = len - i - 1; // 0..5

            if (digit == 0) continue;

            // กรณีพิเศษการอ่าน
            if (pos == 0 && digit == 1 && len > 1) {
                // ตัวท้ายเป็น 1 และมีหลักก่อนหน้า => "เอ็ด"
                result.append("เอ็ด");
            } else if (pos == 1 && digit == 2) {
                // หลักสิบเป็น 2 => "ยี่สิบ"
                result.append("ยี่").append(POSITION_TEXT[pos]);
            } else if (pos == 1 && digit == 1) {
                // หลักสิบเป็น 1 => "สิบ" (ไม่อ่าน "หนึ่งสิบ")
                result.append(POSITION_TEXT[pos]);
            } else {
                result.append(NUMBER_TEXT[digit]).append(POSITION_TEXT[pos]);
            }
        }

        return result.toString();
    }
}