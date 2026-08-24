package com.company.attendance.service.impl;

import com.company.attendance.dto.DepartmentStatsResponse;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.service.DepartmentService;
import com.company.attendance.util.HrPeriod;
import com.company.attendance.util.HrPeriodResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final AnomalyRepository anomalyRepository;
    private final HrPeriodResolver hrPeriodResolver;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public List<DepartmentStatsResponse> findDepartmentStatistics(Integer year, Integer month, Integer hierarchy) {
        HrPeriod period = hrPeriodResolver.resolve(year, month);
        return anomalyRepository.findDepartmentStatistics(period.startDate(), period.endDate(), hierarchy);
    }
}
