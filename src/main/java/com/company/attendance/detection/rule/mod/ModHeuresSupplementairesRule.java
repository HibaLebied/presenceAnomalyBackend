package com.company.attendance.detection.rule.mod;

import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AbstractOvertimeRule;
import org.springframework.stereotype.Component;

@Component
public class ModHeuresSupplementairesRule extends AbstractOvertimeRule {

    @Override
    public HierarchyType hierarchyType() {
        return HierarchyType.MOD;
    }
}
