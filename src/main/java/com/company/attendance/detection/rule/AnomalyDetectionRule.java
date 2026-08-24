package com.company.attendance.detection.rule;

import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.model.HierarchyType;

public interface AnomalyDetectionRule {

    HierarchyType hierarchyType();

    int priority();

    DetectionResult detect(DetectionContext context);
}
