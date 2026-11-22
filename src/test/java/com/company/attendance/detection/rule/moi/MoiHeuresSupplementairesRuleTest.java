package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoiHeuresSupplementairesRuleTest {

    private final MoiHeuresSupplementairesRule rule = new MoiHeuresSupplementairesRule();

    @Test
    void shouldUseTheSameHdThresholdAsMod() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .planning("HD")
                .punches(8 * 3600, 8 * 3600 + 50 * 60)
                .build();

        assertEquals(AnomalyCode.HEURES_SUPPLEMENTAIRES, rule.detect(DetectionContext.from(record)).anomalyCode());
    }

    @Test
    void shouldHandleMoiOvernightPlanning() {
        AttendanceRecord normalRecord = AttendanceRecordTestBuilder.record()
                .moi()
                .planning("SH9B")
                .punches(22 * 3600, 5 * 3600 + 30 * 60)
                .build();
        AttendanceRecord overtimeRecord = AttendanceRecordTestBuilder.record()
                .moi()
                .planning("SH9B")
                .punches(22 * 3600, 7 * 3600)
                .build();

        assertFalse(rule.detect(DetectionContext.from(normalRecord)).hasAnomaly());
        assertEquals(
                AnomalyCode.HEURES_SUPPLEMENTAIRES,
                rule.detect(DetectionContext.from(overtimeRecord)).anomalyCode()
        );
    }

    @Test
    void shouldUseTheSameBoundaryLogicForAllMoiShifts() {
        assertFalse(rule.detect(DetectionContext.from(AttendanceRecordTestBuilder.record()
                .moi().planning("SH3").punches(15 * 3600, 23 * 3600).build())).hasAnomaly());
        assertFalse(rule.detect(DetectionContext.from(AttendanceRecordTestBuilder.record()
                .moi().planning("SH3B").punches(14 * 3600, 22 * 3600).build())).hasAnomaly());
        assertFalse(rule.detect(DetectionContext.from(AttendanceRecordTestBuilder.record()
                .moi().planning("SH1").punches(7 * 3600, 15 * 3600).build())).hasAnomaly());
        assertFalse(rule.detect(DetectionContext.from(AttendanceRecordTestBuilder.record()
                .moi().planning("SH1B").punches(6 * 3600, 14 * 3600).build())).hasAnomaly());

        assertEquals(
                AnomalyCode.HEURES_SUPPLEMENTAIRES,
                rule.detect(DetectionContext.from(AttendanceRecordTestBuilder.record()
                        .moi().planning("SH3").punches(14 * 3600 + 10 * 60, 23 * 3600).build())).anomalyCode()
        );
    }
}
