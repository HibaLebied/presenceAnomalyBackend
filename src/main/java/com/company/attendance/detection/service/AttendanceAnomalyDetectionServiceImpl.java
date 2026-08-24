package com.company.attendance.detection.service;

import com.company.attendance.detection.engine.AnomalyDetectionEngine;
import com.company.attendance.detection.factory.AnomalyFactory;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.entity.dashboard.AnomalyType;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.repository.dashboard.AnomalyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceAnomalyDetectionServiceImpl implements AttendanceAnomalyDetectionService {

    private final AnomalyDetectionEngine anomalyDetectionEngine;
    private final AnomalyFactory anomalyFactory;
    private final AnomalyRepository anomalyRepository;
    private final AnomalyTypeRepository anomalyTypeRepository;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager")
    public void detectForRecords(List<AttendanceRecord> attendanceRecords) {
        attendanceRecords.forEach(this::detectForRecord);
    }

    private void detectForRecord(AttendanceRecord attendanceRecord) {
        anomalyRepository.deleteByAttendanceRecord(attendanceRecord);
        anomalyRepository.flush();

        DetectionResult detectionResult = anomalyDetectionEngine.detect(DetectionContext.from(attendanceRecord));
        detectionResult.anomalyCodeOptional()
                .map(Enum::name)
                .map(this::findAnomalyType)
                .map(anomalyType -> anomalyFactory.create(attendanceRecord, anomalyType))
                .ifPresent(anomalyRepository::save);
    }

    private AnomalyType findAnomalyType(String anomalyCode) {
        return anomalyTypeRepository.findByCode(anomalyCode)
                .orElseThrow(() -> new IllegalStateException("Anomaly type not found: " + anomalyCode));
    }
}
