package com.company.attendance.service.impl;

import com.company.attendance.dto.DashboardResponse;
import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.repository.dashboard.SynchronizationHistoryRepository;
import com.company.attendance.service.DashboardService;
import com.company.attendance.util.HrPeriod;
import com.company.attendance.util.HrPeriodResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final int DASHBOARD_SUPERVISOR_LIMIT = 10;

    private final AnomalyRepository anomalyRepository;
    private final SynchronizationHistoryRepository synchronizationHistoryRepository;
    private final HrPeriodResolver hrPeriodResolver;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public DashboardResponse getDashboard(Integer year, Integer month, Integer hierarchy) {
        HrPeriod period = hrPeriodResolver.resolve(year, month);

        var latestSynchronization =
                synchronizationHistoryRepository
                        .findTopByStatusOrderByEndTimeDesc(SynchronizationStatus.SUCCESS);

        log.info(
                "JVM timezone={}, endTime lu par JPA={}",
                ZoneId.systemDefault(),
                latestSynchronization.map(SynchronizationHistory::getEndTime).orElse(null)
        );

        return new DashboardResponse(
                anomalyRepository.countFiltered(period.startDate(), period.endDate(), hierarchy),
                synchronizationHistoryRepository.findTopByStatusOrderByEndTimeDesc(SynchronizationStatus.SUCCESS)
                        .map(history -> history.getEndTime())
                        .orElse(null),
                anomalyRepository.findDepartmentStatistics(period.startDate(), period.endDate(), hierarchy),
                anomalyRepository.findSupervisorRankingSummary(
                        period.startDate(),
                        period.endDate(),
                        hierarchy,
                        PageRequest.of(0, DASHBOARD_SUPERVISOR_LIMIT)
                )
        );
    }
}
