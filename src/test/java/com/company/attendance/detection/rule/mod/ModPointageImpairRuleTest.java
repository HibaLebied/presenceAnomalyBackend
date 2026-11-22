package com.company.attendance.detection.rule.mod;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModPointageImpairRuleTest {

    private final ModPointageImpairRule rule = new ModPointageImpairRule();

    @Test
    void shouldDetectImpairWhenFlagIsOne() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record().mod().impair(true).build();

        assertEquals(AnomalyCode.POINTAGE_IMPAIR, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldNotDetectImpairWhenFlagIsZero() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record().mod().impair(false).build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }
}
