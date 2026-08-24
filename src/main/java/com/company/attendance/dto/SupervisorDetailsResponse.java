package com.company.attendance.dto;

import java.util.List;

public record SupervisorDetailsResponse(
        String supervisorId,
        long totalAnomalies,
        List<String> departments,
        List<DelayCategoryResponse> delayCategories
) {
}
