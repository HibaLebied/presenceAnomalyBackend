package com.company.attendance.service;

import com.company.attendance.dto.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboard(Integer year, Integer month, Integer hierarchy);
}
