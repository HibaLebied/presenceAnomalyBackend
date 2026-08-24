package com.company.attendance.service;

import com.company.attendance.dto.AnomalyResponse;
import com.company.attendance.dto.AnomalySearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnomalyQueryService {

    Page<AnomalyResponse> findAll(AnomalySearchCriteria criteria, Pageable pageable);
}
