package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AbstractAttendanceRule;
import org.springframework.stereotype.Component;

@Component
public class MoiAbsenceNonSaisieRule extends AbstractAttendanceRule {

    @Override
    public HierarchyType hierarchyType() {
        return HierarchyType.MOI;
    }

    @Override
    public int priority() {
        return 16;
    }

    @Override
    public DetectionResult detect(DetectionContext context) {
        if (isWorkedDayType(context.attendanceRecord())
                && !isSaturday(context.attendanceRecord())
                && punchSum(context.attendanceRecord()) != 0
                && !Boolean.TRUE.equals(context.attendanceRecord().getImpair())
                && isMoiWorkDaysZero(context.attendanceRecord())
                && hasNoAbsence(context.attendanceRecord())) {
            return DetectionResult.detected(AnomalyCode.ABSENCE_NON_SAISIE);
        }

        return DetectionResult.noAnomaly();
    }
}
