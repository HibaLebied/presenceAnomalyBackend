package com.company.attendance.detection.selector;

import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AnomalyDetectionRule;

import java.util.List;

public interface RuleSelector {

    List<AnomalyDetectionRule> selectRules(HierarchyType hierarchyType);
}
