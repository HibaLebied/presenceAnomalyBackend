package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoiPointageImpairRuleTest {

    private final MoiPointageImpairRule rule = new MoiPointageImpairRule();

    @Test
    void shouldDetectImpairWhenFlagIsOne() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record().moi().impair(true).build();

        assertEquals(AnomalyCode.POINTAGE_IMPAIR, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldNotDetectImpairWhenFlagIsZero() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record().moi().impair(false).build();

        assertFalse(rule.detect(DetectionContext.from(record)).hasAnomaly());
    }
}
