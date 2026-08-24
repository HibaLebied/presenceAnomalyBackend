package com.company.attendance.detection.engine;

import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.rule.AnomalyDetectionRule;
import com.company.attendance.detection.selector.RuleSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnomalyDetectionEngineImpl implements AnomalyDetectionEngine {

    private final RuleSelector ruleSelector;

    @Override
    public DetectionResult detect(DetectionContext context) {
        return ruleSelector.selectRules(context.hierarchyType())
                .stream()
                .map(rule -> rule.detect(context))
                .filter(DetectionResult::hasAnomaly)
                .findFirst()
                .orElseGet(DetectionResult::noAnomaly);
    }
}
