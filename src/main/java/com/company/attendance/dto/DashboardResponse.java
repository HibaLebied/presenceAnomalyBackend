package com.company.attendance.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        long totalAnomalies,
        LocalDateTime lastSynchronizationDate,
        List<DepartmentStatsResponse> departmentStatistics,
        List<SupervisorSummaryResponse> supervisorRankingSummary
) {
}
