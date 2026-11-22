package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoiAbsenceNonSaisieRuleTest {

    private final MoiAbsenceNonSaisieRule rule = new MoiAbsenceNonSaisieRule();

    @Test
    void shouldDetectWeekdayWithPunchAndZeroWorkDays() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi("0")
                .absenceType(null)
                .impair(false)
                .build();

        assertEquals(AnomalyCode.ABSENCE_NON_SAISIE, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldNotDetectOnSaturday() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .date(AttendanceRecordTestBuilder.record().build().getDtPointage().with(DayOfWeek.SATURDAY))
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi("0")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenWorkDaysAreGreaterThanZero() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi("1")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectForFractionalWorkDaysGreaterThanZero() {
        for (String workDays : new String[]{"0.25", "0.49", "1.00"}) {
            AttendanceRecord record = AttendanceRecordTestBuilder.record()
                    .moi()
                    .typeJour("T")
                    .planning(null)
                    .punches(8 * 3600, 8 * 3600 + 20 * 60)
                    .nbjTmoi(workDays)
                    .build();

            assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
        }
    }

    @Test
    void shouldNotDetectWhenWorkDaysAreUnknown() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi(null)
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenAbsenceIsEntered() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi("0")
                .absenceType("CONGE")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectForNonWorkedDayTypes() {
        for (String dayType : new String[]{"F", "R", null}) {
            AttendanceRecord record = AttendanceRecordTestBuilder.record()
                    .moi()
                    .typeJour(dayType)
                    .planning(null)
                    .punches(8 * 3600, 8 * 3600 + 20 * 60)
                    .nbjTmoi("0")
                    .build();

            assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
        }
    }

    @Test
    void shouldTreatWhitespaceAbsenceAsEmpty() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .planning(null)
                .punches(8 * 3600, 8 * 3600 + 20 * 60)
                .nbjTmoi("0")
                .absenceType("   ")
                .build();

        assertEquals(AnomalyCode.ABSENCE_NON_SAISIE, rule.detect(DetectionContext.from(record)).anomalyCode());
    }
}
