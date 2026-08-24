package com.company.attendance.detection.model;

import com.company.attendance.entity.dashboard.AttendanceRecord;

public record DetectionContext(
        AttendanceRecord attendanceRecord,
        HierarchyType hierarchyType
) {
    public static DetectionContext from(AttendanceRecord attendanceRecord) {
        return new DetectionContext(
                attendanceRecord,
                HierarchyType.fromCode(attendanceRecord.getHierarchie())
        );
    }
}
