package com.company.attendance.dto;

public record DelayCategoryCounts(
        Long oneDay,
        Long twoToNineDays,
        Long tenPlusDays
) {

    public DelayCategoryCounts {
        oneDay = zeroIfNull(oneDay);
        twoToNineDays = zeroIfNull(twoToNineDays);
        tenPlusDays = zeroIfNull(tenPlusDays);
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
