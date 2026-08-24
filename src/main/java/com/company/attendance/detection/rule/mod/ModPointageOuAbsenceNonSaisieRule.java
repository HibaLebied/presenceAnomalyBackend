package com.company.attendance.detection.rule.mod;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AbstractAttendanceRule;
import org.springframework.stereotype.Component;

@Component
public class ModPointageOuAbsenceNonSaisieRule extends AbstractAttendanceRule {

    @Override
    public HierarchyType hierarchyType() {
        return HierarchyType.MOD;
    }

    @Override
    public int priority() {
        return 15;
    }

    @Override
    public DetectionResult detect(DetectionContext context) {
        if (isWorkedDayType(context.attendanceRecord())
                && punchSum(context.attendanceRecord()) == 0
                && hasNoAbsence(context.attendanceRecord())) {
            return DetectionResult.detected(AnomalyCode.POINTAGE_OU_ABSENCE_NON_SAISIE);
        }

        return DetectionResult.noAnomaly();
    }
}
