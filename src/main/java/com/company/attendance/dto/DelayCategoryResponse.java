package com.company.attendance.dto;

public record DelayCategoryResponse(
        String category,
        long anomalyCount
) {
}
