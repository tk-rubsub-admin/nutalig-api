package com.nutalig.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskingUtilTest {

    @Test
    void maskText_shouldReturnCorrectData() {
        // When
        final String result = MaskingUtil.maskText("1234567890");

        // Then
        assertEquals("12345xxxx", result);
    }

    @Test
    void maskText_whenInputNull_shouldReturnCorrectData() {
        // When & Then
        assertNull(MaskingUtil.maskText(null));
    }
}