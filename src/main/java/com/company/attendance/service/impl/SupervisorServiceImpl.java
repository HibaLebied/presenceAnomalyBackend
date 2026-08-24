package com.company.attendance.service.impl;

import com.company.attendance.dto.DelayCategoryCounts;
import com.company.attendance.dto.DelayCategoryResponse;
import com.company.attendance.dto.SupervisorDetailsResponse;
import com.company.attendance.dto.SupervisorSummaryResponse;
import com.company.attendance.exception.ResourceNotFoundException;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.service.SupervisorService;
import com.company.attendance.util.HrPeriod;
import com.company.attendance.util.HrPeriodResolver;
import com.company.attendance.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupervisorServiceImpl implements SupervisorService {

    private static final String ONE_DAY = "-1 Day";
    private static final String TWO_TO_NINE_DAYS = "2\u20139 Days";
    private static final String TEN_PLUS_DAYS = "+10 Days";

    private final AnomalyRepository anomalyRepository;
    private final HrPeriodResolver hrPeriodResolver;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public Page<SupervisorSummaryResponse> findSupervisors(
            String department,
            Integer year,
            Integer month,
            Integer hierarchy,
            Pageable pageable
    ) {
        HrPeriod period = hrPeriodResolver.resolve(year, month);

        return anomalyRepository.findSupervisorRanking(
                TextUtils.trimToNull(department),
                period.startDate(),
                period.endDate(),
                hierarchy,
                pageable
        );
    }

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public SupervisorDetailsResponse findSupervisorDetails(
            String supervisorId,
            Integer year,
            Integer month,
            Integer hierarchy
    ) {
        HrPeriod period = hrPeriodResolver.resolve(year, month);
        long totalAnomalies = anomalyRepository.countBySupervisorId(
                supervisorId,
                period.startDate(),
                period.endDate(),
                hierarchy
        );
        if (totalAnomalies == 0) {
            throw new ResourceNotFoundException("Supervisor '%s' was not found".formatted(supervisorId));
        }

        LocalDate today = LocalDate.now(clock);
        DelayCategoryCounts counts = anomalyRepository.countDelayCategoriesBySupervisorId(
                supervisorId,
                today.minusDays(1),
                today.minusDays(9),
                today.minusDays(2),
                today.minusDays(10),
                period.startDate(),
                period.endDate(),
                hierarchy
        );

        List<DelayCategoryResponse> delayCategories = List.of(
                new DelayCategoryResponse(ONE_DAY, counts.oneDay()),
                new DelayCategoryResponse(TWO_TO_NINE_DAYS, counts.twoToNineDays()),
                new DelayCategoryResponse(TEN_PLUS_DAYS, counts.tenPlusDays())
        );

        List<String> departments = anomalyRepository.findDepartmentsBySupervisorId(
                supervisorId,
                period.startDate(),
                period.endDate(),
                hierarchy
        );

        return new SupervisorDetailsResponse(supervisorId, totalAnomalies, departments, delayCategories);
    }
}
