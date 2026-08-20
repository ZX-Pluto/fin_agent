package com.huawei.fin.ai.material.common.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private TextUtil() {
    }

    public static boolean containsAny(String text, String... keys) {
        if (text == null) {
            return false;
        }
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static BigDecimal extractNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group());
        }
        return null;
    }

    public static String extractFirstLine(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return "";
    }
}
