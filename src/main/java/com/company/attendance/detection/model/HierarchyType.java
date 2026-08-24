package com.company.attendance.detection.model;

import java.util.Arrays;

public enum HierarchyType {
    MOI(1),
    MOD(2);

    private final int code;

    HierarchyType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static HierarchyType fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported hierarchy code: " + code));
    }
}
