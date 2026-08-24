package com.company.attendance.service;

import com.company.attendance.dto.SupervisorDetailsResponse;
import com.company.attendance.dto.SupervisorSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupervisorService {

    Page<SupervisorSummaryResponse> findSupervisors(
            String department,
            Integer year,
            Integer month,
            Integer hierarchy,
            Pageable pageable
    );

    SupervisorDetailsResponse findSupervisorDetails(
            String supervisorId,
            Integer year,
            Integer month,
            Integer hierarchy
    );
}
