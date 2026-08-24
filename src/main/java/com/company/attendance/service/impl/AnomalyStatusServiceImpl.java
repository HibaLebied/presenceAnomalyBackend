package com.company.attendance.service.impl;

import com.company.attendance.dto.AnomalyResponse;
import com.company.attendance.entity.dashboard.Anomaly;
import com.company.attendance.entity.dashboard.AnomalyStatus;
import com.company.attendance.exception.ResourceNotFoundException;
import com.company.attendance.mapper.AnomalyContractMapper;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.service.AnomalyStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnomalyStatusServiceImpl implements AnomalyStatusService {

    private final AnomalyRepository anomalyRepository;
    private final AnomalyContractMapper anomalyContractMapper;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager")
    public AnomalyResponse ignore(Long anomalyId) {
        return updateStatus(anomalyId, AnomalyStatus.IGNORED);
    }

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager")
    public AnomalyResponse activate(Long anomalyId) {
        return updateStatus(anomalyId, AnomalyStatus.ACTIVE);
    }

    private AnomalyResponse updateStatus(Long anomalyId, AnomalyStatus status) {
        Anomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly '%d' was not found".formatted(anomalyId)));

        anomaly.setStatus(status);
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        return anomalyContractMapper.toResponse(savedAnomaly);
    }
}
