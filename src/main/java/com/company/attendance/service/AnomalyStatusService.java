package com.company.attendance.service;

import com.company.attendance.dto.AnomalyResponse;

public interface AnomalyStatusService {

    AnomalyResponse ignore(Long anomalyId);

    AnomalyResponse activate(Long anomalyId);
}
