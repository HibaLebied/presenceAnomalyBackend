package com.company.attendance.detection.service;

import com.company.attendance.entity.dashboard.AttendanceRecord;

import java.util.List;

public interface AttendanceAnomalyDetectionService {

    void detectForRecords(List<AttendanceRecord> attendanceRecords);
}
