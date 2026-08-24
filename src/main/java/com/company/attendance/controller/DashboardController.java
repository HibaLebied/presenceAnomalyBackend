package com.company.attendance.controller;

import com.company.attendance.dto.DashboardResponse;
import com.company.attendance.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard(
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
        return dashboardService.getDashboard(year, month, hierarchy);

    }
}
