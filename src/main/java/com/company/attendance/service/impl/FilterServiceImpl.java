package com.company.attendance.service.impl;

import com.company.attendance.repository.dashboard.AttendanceRecordRepository;
import com.company.attendance.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FilterServiceImpl implements FilterService {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public List<Integer> findAvailableYears() {
        return attendanceRecordRepository.findAvailableYears();
    }
}
