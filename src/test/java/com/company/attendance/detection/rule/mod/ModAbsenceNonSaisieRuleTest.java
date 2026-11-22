package com.company.attendance.detection.rule.mod;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModAbsenceNonSaisieRuleTest {

    private final ModAbsenceNonSaisieRule rule = new ModAbsenceNonSaisieRule();

    @Test
    void shouldDetectShortWorkedDayWithoutAbsence() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .planning("SH3")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod("0.50")
                .absenceType(null)
                .impair(false)
                .build();

        assertEquals(AnomalyCode.ABSENCE_NON_SAISIE, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldNotDetectWhenWorkDurationIsGreaterThanHalfDay() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod("0.51")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldDetectWhenWorkDurationIsLessThanHalfDay() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod("0.49")
                .build();

        assertEquals(AnomalyCode.ABSENCE_NON_SAISIE, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldNotDetectWhenWorkDurationIsUnknown() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod(null)
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenImpairIsOne() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod("0.50")
                .impair(true)
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenAbsenceIsEntered() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .punches(15 * 3600, 17 * 3600)
                .nbhTmod("0.50")
                .absenceType("CONGE")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenThereAreNoPunches() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .typeJour("T")
                .noPunches()
                .nbhTmod("0.50")
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectForNonWorkedDayTypes() {
        for (String dayType : new String[]{"F", "R", null}) {
            AttendanceRecord record = AttendanceRecordTestBuilder.record()
                    .mod()
                    .typeJour(dayType)
                    .punches(15 * 3600, 17 * 3600)
                    .nbhTmod("0.50")
                    .build();

            assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
        }
    }
}
