package com.company.attendance.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
