package com.company.attendance.detection.engine;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.rule.AnomalyDetectionRule;
import com.company.attendance.detection.rule.mod.ModAbsenceNonSaisieRule;
import com.company.attendance.detection.rule.mod.ModHeuresSupplementairesRule;
import com.company.attendance.detection.rule.mod.ModPointageImpairRule;
import com.company.attendance.detection.rule.mod.ModPointageOuAbsenceNonSaisieRule;
import com.company.attendance.detection.rule.moi.MoiAbsenceNonSaisieRule;
import com.company.attendance.detection.rule.moi.MoiHeuresSupplementairesRule;
import com.company.attendance.detection.rule.moi.MoiPointageImpairRule;
import com.company.attendance.detection.rule.moi.MoiPointageOuAbsenceNonSaisieRule;
import com.company.attendance.detection.selector.HierarchyRuleSelector;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyDetectionEngineTest {

    private HierarchyRuleSelector selector;
    private AnomalyDetectionEngine engine;

    @BeforeEach
    void setUp() {
        List<AnomalyDetectionRule> rules = List.of(
                new ModPointageImpairRule(),
                new ModPointageOuAbsenceNonSaisieRule(),
                new ModAbsenceNonSaisieRule(),
                new ModHeuresSupplementairesRule(),
                new MoiPointageImpairRule(),
                new MoiPointageOuAbsenceNonSaisieRule(),
                new MoiAbsenceNonSaisieRule(),
                new MoiHeuresSupplementairesRule()
        );

        selector = new HierarchyRuleSelector(rules);
        engine = new AnomalyDetectionEngineImpl(selector);
    }

    @Test
    void shouldSelectOnlyModRulesForModHierarchy() {
        assertEquals(4, selector.selectRules(com.company.attendance.detection.model.HierarchyType.MOD).size());
        assertTrue(selector.selectRules(com.company.attendance.detection.model.HierarchyType.MOD)
                .stream()
                .allMatch(rule -> rule.hierarchyType() == com.company.attendance.detection.model.HierarchyType.MOD));
    }

    @Test
    void shouldSelectOnlyMoiRulesForMoiHierarchy() {
        assertEquals(4, selector.selectRules(com.company.attendance.detection.model.HierarchyType.MOI).size());
        assertTrue(selector.selectRules(com.company.attendance.detection.model.HierarchyType.MOI)
                .stream()
                .allMatch(rule -> rule.hierarchyType() == com.company.attendance.detection.model.HierarchyType.MOI));
    }

    @Test
    void shouldReturnImpairBeforeOtherApplicableRules() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .planning("HD")
                .punches(8 * 3600, 10 * 3600)
                .impair(true)
                .build();

        assertEquals(AnomalyCode.POINTAGE_IMPAIR, detect(record).anomalyCode());
    }

    @Test
    void shouldReturnImpairForMoiHierarchyToo() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .moi()
                .planning("HD")
                .punches(8 * 3600, 10 * 3600)
                .impair(true)
                .build();

        assertEquals(AnomalyCode.POINTAGE_IMPAIR, detect(record).anomalyCode());
    }

    @Test
    void shouldReturnNoAnomalyForNormalModShift() {
        AttendanceRecord record = AttendanceRecordTestBuilder.record()
                .mod()
                .planning("SH3")
                .punches(15 * 3600, 23 * 3600)
                .build();

        assertFalse(detect(record).hasAnomaly());
    }

    private DetectionResult detect(AttendanceRecord record) {
        return engine.detect(DetectionContext.from(record));
    }
}
