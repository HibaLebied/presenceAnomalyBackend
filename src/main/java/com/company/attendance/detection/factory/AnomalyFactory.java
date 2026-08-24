package com.company.attendance.detection.factory;

import com.company.attendance.entity.dashboard.Anomaly;
import com.company.attendance.entity.dashboard.AnomalyType;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.springframework.stereotype.Component;

@Component
public class AnomalyFactory {

    public Anomaly create(AttendanceRecord attendanceRecord, AnomalyType anomalyType) {
        return Anomaly.builder()
                .attendanceRecord(attendanceRecord)
                .anomalyType(anomalyType)
                .build();
    }
}
