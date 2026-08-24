package com.company.attendance.detection.selector;

import com.company.attendance.detection.model.HierarchyType;
import com.company.attendance.detection.rule.AnomalyDetectionRule;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class HierarchyRuleSelector implements RuleSelector {

    private final List<AnomalyDetectionRule> rules;

    public HierarchyRuleSelector(List<AnomalyDetectionRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(AnomalyDetectionRule::priority))
                .toList();
    }

    @Override
    public List<AnomalyDetectionRule> selectRules(HierarchyType hierarchyType) {
        return rules.stream()
                .filter(rule -> rule.hierarchyType() == hierarchyType)
                .toList();
    }
}
