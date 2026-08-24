package com.company.attendance.detection.model;

import java.util.Optional;

public record DetectionResult(AnomalyCode anomalyCode) {

    public static DetectionResult detected(AnomalyCode anomalyCode) {
        return new DetectionResult(anomalyCode);
    }

    public static DetectionResult noAnomaly() {
        return new DetectionResult(null);
    }

    public boolean hasAnomaly() {
        return anomalyCode != null;
    }

    public Optional<AnomalyCode> anomalyCodeOptional() {
        return Optional.ofNullable(anomalyCode);
    }
}
