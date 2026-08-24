package com.company.attendance.dto;

import com.company.attendance.entity.dashboard.SynchronizationStatus;

import java.time.LocalDateTime;

public record SynchronizationStatusDTO(
        Long synchronizationId,
        SynchronizationStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime lastSyncTime,
        int recordsInserted,
        int recordsUpdated,
        int totalProcessed,
        String errorMessage
) {
}
