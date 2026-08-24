package com.company.attendance.dto.pagination;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        PageMetadata page
) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                new PageMetadata(
                        source.getNumber(),
                        source.getSize(),
                        source.getNumberOfElements(),
                        source.getTotalElements(),
                        source.getTotalPages(),
                        source.isFirst(),
                        source.isLast(),
                        source.isEmpty()
                )
        );
    }
}
