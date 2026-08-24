package com.company.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnomalyResponse(
        Long id,
        String employeeId,
        String employeeName,
        String supervisorId,
        String department,
        LocalDate attendanceDate,
        Integer hierarchyCode,
        String hierarchyLabel,
        String anomalyTypeCode,
        String anomalyTypeName,
        String status,
        Integer entry1,
        Integer exit1,
        Integer entry2,
        Integer exit2,
        Integer entry3,
        Integer exit3,
        LocalDateTime synchronizedAt,
        long delayDays
) {
}
