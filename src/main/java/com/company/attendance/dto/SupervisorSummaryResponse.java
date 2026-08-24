package com.company.attendance.dto;

public record SupervisorSummaryResponse(
        String supervisorId,
        long anomalyCount
) {
}
