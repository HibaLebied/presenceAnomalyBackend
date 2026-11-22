package com.company.attendance.detection.rule.mod;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModHeuresSupplementairesRuleTest {

    private final ModHeuresSupplementairesRule rule = new ModHeuresSupplementairesRule();

    @Test
    void shouldNotDetectHdWhenWorkDurationIsLessThanFiftyMinutes() {
        AttendanceRecord record = record("HD", 8 * 3600, 8 * 3600 + 49 * 60);

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldDetectHdAtExactlyFiftyMinutes() {
        AttendanceRecord record = record("HD", 8 * 3600, 8 * 3600 + 50 * 60);

        assertOvertime(record);
    }

    @Test
    void shouldNotDetectWhenOvertimeIsAlreadyDeclared() {
        AttendanceRecord record = record("HD", 8 * 3600, 10 * 3600);
        record.setDebutHsup(8 * 3600);

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldIgnoreNullOrBlankPlanning() {
        AttendanceRecord nullPlanning = record(null, 8 * 3600, 10 * 3600);
        AttendanceRecord blankPlanning = record("   ", 8 * 3600, 10 * 3600);

        assertFalse(rule.detect(DetectionContext.from(nullPlanning)).hasAnomaly());
        assertFalse(rule.detect(DetectionContext.from(blankPlanning)).hasAnomaly());
    }

    @Test
    void shouldAcceptNormalShifts() {
        assertNoAnomaly(record("SH3", 15 * 3600, 23 * 3600));
        assertNoAnomaly(record("SH3B", 14 * 3600, 22 * 3600));
        assertNoAnomaly(record("SH1", 7 * 3600, 15 * 3600));
        assertNoAnomaly(record("SH1B", 6 * 3600, 14 * 3600));
        assertNoAnomaly(record("SH9", 23 * 3600, 6 * 3600 + 30 * 60));
        assertNoAnomaly(record("SH9B", 22 * 3600, 5 * 3600 + 30 * 60));
    }

    @Test
    void shouldDetectEntryFiftyMinutesBeforeSh3() {
        assertOvertime(record("SH3", 14 * 3600 + 10 * 60, 23 * 3600));
    }

    @Test
    void shouldDetectExitFiftyMinutesAfterSh3() {
        assertOvertime(record("SH3", 15 * 3600, 23 * 3600 + 50 * 60));
    }

    @Test
    void shouldDetectBoundaryOvertimeForOtherDayShifts() {
        assertOvertime(record("SH3B", 13 * 3600 + 10 * 60, 22 * 3600));
        assertOvertime(record("SH3B", 14 * 3600, 22 * 3600 + 50 * 60));
        assertOvertime(record("SH1", 6 * 3600 + 10 * 60, 15 * 3600));
        assertOvertime(record("SH1", 7 * 3600, 15 * 3600 + 50 * 60));
        assertOvertime(record("SH1B", 5 * 3600 + 10 * 60, 14 * 3600));
        assertOvertime(record("SH1B", 6 * 3600, 14 * 3600 + 50 * 60));
    }

    @Test
    void shouldHandleSh9OvernightExit() {
        assertNoAnomaly(record("SH9", 23 * 3600, 6 * 3600 + 30 * 60));
        assertOvertime(record("SH9", 23 * 3600, 13 * 3600));
        assertNoAnomaly(record("SH9", 23 * 3600, 23 * 3600 + 43 * 60));
    }

    @Test
    void shouldHandleSh9bOvernightExit() {
        assertNoAnomaly(record("SH9B", 22 * 3600, 5 * 3600 + 30 * 60));
        assertOvertime(record("SH9B", 22 * 3600, 7 * 3600));
        assertOvertime(record("SH9B", 21 * 3600 + 10 * 60, 6 * 3600));
    }

    @Test
    void shouldDetectExactlyFiftyMinutesBeforeAndAfterSh9() {
        assertOvertime(record("SH9", 22 * 3600 + 10 * 60, 7 * 3600));
        assertOvertime(record("SH9", 23 * 3600, 7 * 3600 + 50 * 60));
    }

    @Test
    void shouldNormalizeExitAfterMidnightForSh3() {
        assertOvertime(record("SH3", 15 * 3600, 0));
    }

    @Test
    void shouldUseFirstEntryAndLastExitForMultiplePunches() {
        AttendanceRecord record = record("SH3", 15 * 3600, 18 * 3600);
        record.setE2(19 * 3600);
        record.setS2(23 * 3600);

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());

        record.setS2(23 * 3600 + 50 * 60);

        assertOvertime(record);
    }

    @Test
    void shouldUseThirdEntryAndThirdExitWhenEarlierPunchesAreMissing() {
        AttendanceRecord record = record("SH3", null, null);
        record.setE3(15 * 3600);
        record.setS3(23 * 3600);

        assertNoAnomaly(record);

        record.setS3(23 * 3600 + 50 * 60);

        assertOvertime(record);
    }

    @Test
    void shouldHandleOnlyEntryOrOnlyExit() {
        AttendanceRecord onlyEntry = record("SH3", 14 * 3600 + 10 * 60, null);
        AttendanceRecord onlyExit = record("SH3", null, 23 * 3600 + 50 * 60);

        assertOvertime(onlyEntry);
        assertOvertime(onlyExit);
    }

    @Test
    void shouldTreatZeroAsARealMidnightPunch() {
        AttendanceRecord record = record("SH3", 0, 23 * 3600);

        assertOvertime(record);
    }

    @Test
    void shouldIgnoreImpairForOvertimeDetection() {
        AttendanceRecord record = record("HD", 8 * 3600, 10 * 3600);
        record.setImpair(true);

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenDeclaredOvertimeIsZeroButPresent() {
        AttendanceRecord record = record("HD", 8 * 3600, 10 * 3600);
        record.setDebutHsup(0);

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    private AttendanceRecord record(String planning, Integer entry, Integer exit) {
        return AttendanceRecordTestBuilder.record()
                .mod()
                .planning(planning)
                .punches(entry, exit)
                .build();
    }

    private void assertOvertime(AttendanceRecord record) {
        assertEquals(
                AnomalyCode.HEURES_SUPPLEMENTAIRES,
                rule.detect(DetectionContext.from(record)).anomalyCode()
        );
    }

    private void assertNoAnomaly(AttendanceRecord record) {
        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }
}
