package com.company.attendance.detection.engine;

import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;

public interface AnomalyDetectionEngine {

    DetectionResult detect(DetectionContext context);
}
