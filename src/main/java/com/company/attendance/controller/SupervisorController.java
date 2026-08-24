package com.company.attendance.controller;

import com.company.attendance.dto.SupervisorDetailsResponse;
import com.company.attendance.dto.SupervisorSummaryResponse;
import com.company.attendance.dto.pagination.PageResponse;
import com.company.attendance.service.SupervisorService;
import com.company.attendance.util.PageableUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/supervisors")
@RequiredArgsConstructor
public class SupervisorController {

    private final SupervisorService supervisorService;
    private final PageableUtils pageableUtils;

    @GetMapping
    public PageResponse<SupervisorSummaryResponse> findSupervisors(
            @RequestParam(required = false)
            @Size(max = 120)
            String department,

            @RequestParam(required = false)
            @Min(2000)
            Integer year,

            @RequestParam(required = false)
            @Min(1)
            @Max(12)
            Integer month,

            @RequestParam(required = false)
            @Min(1)
            @Max(2)
            Integer hierarchy,

            @ParameterObject
            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        Page<SupervisorSummaryResponse> supervisors = supervisorService.findSupervisors(
                department,
                year,
                month,
                hierarchy,
                pageableUtils.sanitizeSupervisorPageable(pageable)
        );
        return PageResponse.from(supervisors);
    }

    @GetMapping("/{id}")
    public SupervisorDetailsResponse findSupervisorDetails(
            @PathVariable("id")
            @Size(max = 200)
            String supervisorId,

            @RequestParam(required = false)
            @Min(2000)
            Integer year,

            @RequestParam(required = false)
            @Min(1)
            @Max(12)
            Integer month,

            @RequestParam(required = false)
            @Min(1)
            @Max(2)
            Integer hierarchy
    ) {
        return supervisorService.findSupervisorDetails(supervisorId, year, month, hierarchy);
    }
}
