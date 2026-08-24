package com.company.attendance.detection.rule.moi;

import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AbstractOvertimeRule;
import org.springframework.stereotype.Component;

@Component
public class MoiHeuresSupplementairesRule extends AbstractOvertimeRule {

    @Override
    public HierarchyType hierarchyType() {
        return HierarchyType.MOI;
    }
}
