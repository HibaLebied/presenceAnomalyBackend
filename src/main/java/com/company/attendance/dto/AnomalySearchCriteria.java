package com.company.attendance.dto;

public record AnomalySearchCriteria(
        String employeeId,
        String employeeName,
        String department,
        String supervisorId,
        Integer year,
        Integer month,
        Integer hierarchy,
        boolean includeIgnored
) {
}
