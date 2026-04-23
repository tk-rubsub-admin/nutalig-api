package com.nutalig.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomUtil {

    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateJobId(LocalDate date, int length) {
        String prefix = date.format(DateUtil.YYYY_MM_2);
        // Random between 1 and 100
        int randomNumber = RANDOM.nextInt(100) + 1;

        // Calculate how many digits to pad
        int padLength = length - prefix.length();
        if (padLength < 1) {
            throw new IllegalArgumentException("Length too short for prefix");
        }

        // Pad with zeros to fit the total length
        String paddedRandom = String.format("%0" + padLength + "d", randomNumber);

        return prefix + paddedRandom;
    }

    public static String generateShortCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
        }
        return sb.toString();
    }
}
