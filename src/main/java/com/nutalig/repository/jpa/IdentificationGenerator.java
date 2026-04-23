package com.nutalig.repository.jpa;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class IdentificationGenerator {

    private static final SecureRandom random = new SecureRandom();
    protected static final char[] ALPHANUMERIC_RANDOM_SET = new char[]{
            'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P',
            'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '0'
    };
    public static final List<String> YEARS = List.of(
            "7W", "Q4", "Q6", "PV", "HR", "8C", "YD", "YF", "8F", "8H",
            "8K", "YN", "8P", "8R", "Z4", "YT", "3M", "Z7", "R0", "8W",
            "Q0", "8Y", "R6", "R8", "ZD", "XZ", "ZV", "Y2", "1R", "S5",
            "RT", "1T", "JM", "N6", "S8", "M7", "1W", "K2", "K4", "K6",
            "JV", "SG", "2J", "2S", "T7", "KN", "T8", "2V", "P8", "2Y",
            "CK", "TF", "CX", "LL", "3T", "4G", "3W", "LQ", "DJ", "3Y",
            "3Z", "DL", "M6", "LV", "4C", "4D", "MD", "V3", "B2", "V5",
            "MT", "F3", "5J", "6W", "W2", "R2", "W4", "6Y", "W6", "NR",
            "5Y", "NV", "WD", "FT", "G5", "FV", "G8", "FW", "6L", "X3",
            "WS", "X5", "6T", "P1", "WY", "P2", "WZ", "GM", "H0", "GS"
    );
    public static final List<String> KEYS = List.of(
            "B", "C", "D", "F", "G", "H", "J", "K", "L", "M", "N",
            "P", "Q", "R", "S", "T", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private static final int MAGIC_NUMBER = 2023;

    private Clock clock;

    public IdentificationGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate(String prefix, int length) {
        final LocalDate now = LocalDate.now(clock);
        return prefix + "-"
                + YEARS.get(now.getYear() - MAGIC_NUMBER)
                + KEYS.get(now.getMonthValue() - 1)
                + KEYS.get(now.getDayOfMonth() - 1)
                + random(length);
    }

    private static String random(int length) {
        return RandomStringUtils.random(length, 0, 0, true, true, ALPHANUMERIC_RANDOM_SET, random);
    }

}
