package com.company.attendance.detection.rule;

import com.company.attendance.entity.dashboard.AttendanceRecord;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.stream.Stream;

public abstract class AbstractAttendanceRule implements AnomalyDetectionRule {

    private static final BigDecimal HALF_DAY_THRESHOLD = new BigDecimal("0.5");

    protected boolean hasAnyPunch(AttendanceRecord record) {
        return record.getE1() != null
                || record.getS1() != null
                || record.getE2() != null
                || record.getS2() != null
                || record.getE3() != null
                || record.getS3() != null;
    }

    protected boolean hasNoPunch(AttendanceRecord record) {
        return !hasAnyPunch(record);
    }

    protected boolean hasNoAbsence(AttendanceRecord record) {
        return record.getTypeAbsense() == null || record.getTypeAbsense().isBlank();
    }

    protected boolean isWorkingDayForMoi(AttendanceRecord record) {
        return isPositive(record.getNbjTmoi());
    }

    protected boolean isWorkingDayForMod(AttendanceRecord record) {
        return isPositive(record.getNbhTmod());
    }

    protected boolean isWorkedDayType(AttendanceRecord record) {
        return "T".equals(record.getTypeJour());
    }

    protected int punchSum(AttendanceRecord record) {
        return Stream.of(record.getE1(), record.getS1(), record.getE2(), record.getS2(), record.getE3(), record.getS3())
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    protected boolean isModWorkDurationLessThanHalfDay(AttendanceRecord record) {
        return record.getNbhTmod() != null && record.getNbhTmod().compareTo(HALF_DAY_THRESHOLD) <= 0;
    }

    protected boolean isMoiWorkDaysZero(AttendanceRecord record) {
        return record.getNbjTmoi() != null && record.getNbjTmoi().compareTo(BigDecimal.ZERO) == 0;
    }

    protected boolean isSaturday(AttendanceRecord record) {
        return record.getDtPointage() != null
                && record.getDtPointage().getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    protected Integer firstEntry(AttendanceRecord record) {
        return Stream.of(record.getE1(), record.getE2(), record.getE3())
                .filter(entry -> entry != null)
                .findFirst()
                .orElse(null);
    }

    protected Integer lastExit(AttendanceRecord record) {
        return Stream.of(record.getS3(), record.getS2(), record.getS1())
                .filter(exit -> exit != null)
                .findFirst()
                .orElse(null);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
