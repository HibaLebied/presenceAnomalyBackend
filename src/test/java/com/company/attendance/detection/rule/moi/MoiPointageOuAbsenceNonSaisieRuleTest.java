package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoiPointageOuAbsenceNonSaisieRuleTest {

    private final MoiPointageOuAbsenceNonSaisieRule rule = new MoiPointageOuAbsenceNonSaisieRule();

    @Test
    void shouldDetectWeekdayWithoutPunchAndWithoutAbsence() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .noPunches()
                .absenceType(null)
                .build();

        assertEquals(
                AnomalyCode.POINTAGE_OU_ABSENCE_NON_SAISIE,
                rule.detect(DetectionContext.from(record)).anomalyCode()
        );
    }

    @Test
    void shouldNotDetectOnSaturday() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .date(AttendanceRecordTestBuilder.record().build().getDtPointage().with(DayOfWeek.SATURDAY))
                .typeJour("T")
                .noPunches()
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenPunchExists() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .punches(8 * 3600, 9 * 3600)
                .build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }

    @Test
    void shouldNotDetectWhenAbsenceIsEntered() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .noPunches()
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
                    .noPunches()
                    .build();

            assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
        }
    }

    @Test
    void shouldTreatWhitespaceAbsenceAsEmpty() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .typeJour("T")
                .noPunches()
                .absenceType("   ")
                .build();

        assertEquals(
                AnomalyCode.POINTAGE_OU_ABSENCE_NON_SAISIE,
                rule.detect(DetectionContext.from(record)).anomalyCode()
        );
    }
}
