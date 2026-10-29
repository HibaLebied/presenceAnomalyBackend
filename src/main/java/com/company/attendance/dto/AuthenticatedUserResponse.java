package com.company.attendance.dto;

public record AuthenticatedUserResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
}