package com.company.attendance.controller;

import com.company.attendance.dto.DepartmentStatsResponse;
import com.company.attendance.service.DepartmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentStatsResponse> findDepartments(
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
        return departmentService.findDepartmentStatistics(year, month, hierarchy);
    }
}
