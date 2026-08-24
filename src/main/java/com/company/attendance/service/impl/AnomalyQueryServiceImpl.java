package com.company.attendance.service.impl;

import com.company.attendance.dto.AnomalyResponse;
import com.company.attendance.dto.AnomalySearchCriteria;
import com.company.attendance.mapper.AnomalyContractMapper;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.service.AnomalyQueryService;
import com.company.attendance.util.HrPeriod;
import com.company.attendance.util.HrPeriodResolver;
import com.company.attendance.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnomalyQueryServiceImpl implements AnomalyQueryService {

    private final AnomalyRepository anomalyRepository;
    private final AnomalyContractMapper anomalyContractMapper;
    private final HrPeriodResolver hrPeriodResolver;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public Page<AnomalyResponse> findAll(AnomalySearchCriteria criteria, Pageable pageable) {
        HrPeriod period = hrPeriodResolver.resolve(criteria.year(), criteria.month());

        return anomalyRepository.searchAnomalies(
                        TextUtils.trimToNull(criteria.employeeId()),
                        TextUtils.trimToNull(criteria.employeeName()),
                        TextUtils.trimToNull(criteria.department()),
                        TextUtils.trimToNull(criteria.supervisorId()),
                        period.startDate(),
                        period.endDate(),
                        criteria.hierarchy(),
                        criteria.includeIgnored(),
                        pageable
                )
                .map(anomalyContractMapper::toResponse);
    }
}
