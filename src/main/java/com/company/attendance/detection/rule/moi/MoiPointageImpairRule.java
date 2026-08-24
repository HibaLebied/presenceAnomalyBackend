package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AbstractAttendanceRule;
import org.springframework.stereotype.Component;

@Component
public class MoiPointageImpairRule extends AbstractAttendanceRule {

    @Override
    public HierarchyType hierarchyType() {
        return HierarchyType.MOI;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public DetectionResult detect(DetectionContext context) {
        if (Boolean.TRUE.equals(context.attendanceRecord().getImpair())) {
            return DetectionResult.detected(AnomalyCode.POINTAGE_IMPAIR);
        }

        return DetectionResult.noAnomaly();
    }
}
