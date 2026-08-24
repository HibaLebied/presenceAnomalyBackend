package com.company.attendance.controller;

import com.company.attendance.dto.AnomalyResponse;
import com.company.attendance.dto.AnomalySearchCriteria;
import com.company.attendance.dto.pagination.PageResponse;
import com.company.attendance.service.AnomalyQueryService;
import com.company.attendance.service.AnomalyStatusService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyQueryService anomalyQueryService;
    private final AnomalyStatusService anomalyStatusService;
    private final PageableUtils pageableUtils;

    @GetMapping
    public PageResponse<AnomalyResponse> findAll(
            @RequestParam(required = false)
            @Size(max = 64)
            String employeeId,

            @RequestParam(required = false)
            @Size(max = 150)
            String employeeName,

            @RequestParam(required = false)
            @Size(max = 120)
            String department,

            @RequestParam(required = false)
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
            Integer hierarchy,

            @RequestParam(defaultValue = "false")
            boolean includeIgnored,

            @ParameterObject
            @PageableDefault(size = 20, sort = "attendanceDate")
            Pageable pageable
    ) {
        AnomalySearchCriteria criteria = new AnomalySearchCriteria(
                employeeId,
                employeeName,
                department,
                supervisorId,
                year,
                month,
                hierarchy,
                includeIgnored
        );
        Page<AnomalyResponse> anomalies = anomalyQueryService.findAll(
                criteria,
                pageableUtils.sanitizeAnomalyPageable(pageable)
        );
        return PageResponse.from(anomalies);
    }

    @PatchMapping("/{id}/ignore")
    public AnomalyResponse ignore(@PathVariable("id") Long anomalyId) {
        return anomalyStatusService.ignore(anomalyId);
    }

    @PatchMapping("/{id}/activate")
    public AnomalyResponse activate(@PathVariable("id") Long anomalyId) {
        return anomalyStatusService.activate(anomalyId);
    }
}
