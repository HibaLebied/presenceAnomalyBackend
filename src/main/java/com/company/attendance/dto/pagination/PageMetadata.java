package com.company.attendance.dto.pagination;

public record PageMetadata(
        int page,
        int size,
        int numberOfElements,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
}
