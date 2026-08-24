package com.company.attendance.service;

import com.company.attendance.dto.DepartmentStatsResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentStatsResponse> findDepartmentStatistics(Integer year, Integer month, Integer hierarchy);
}
