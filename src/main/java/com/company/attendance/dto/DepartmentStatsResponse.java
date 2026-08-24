package com.company.attendance.dto;

public record DepartmentStatsResponse(
        String department,
        long anomalyCount
) {
}
