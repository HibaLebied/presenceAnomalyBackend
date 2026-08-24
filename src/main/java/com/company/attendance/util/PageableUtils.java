package com.company.attendance.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PageableUtils {

    private static final int MAX_PAGE_SIZE = 1000;

    private static final Map<String, String> ANOMALY_SORT_MAPPING = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("employeeId", "attendanceRecord.matricule"),
            Map.entry("employeeName", "attendanceRecord.nom"),
            Map.entry("supervisorId", "attendanceRecord.superieur"),
            Map.entry("department", "attendanceRecord.departement"),
            Map.entry("attendanceDate", "attendanceRecord.dtPointage"),
            Map.entry("hierarchyCode", "attendanceRecord.hierarchie"),
            Map.entry("anomalyTypeCode", "anomalyType.code"),
            Map.entry("startDate", "attendanceRecord.dtPointage"),
            Map.entry("importDate", "attendanceRecord.synchronizedAt"),
            Map.entry("synchronizedAt", "attendanceRecord.synchronizedAt"),
            Map.entry("delayDays", "attendanceRecord.dtPointage")
    );

    public Pageable sanitizeAnomalyPageable(Pageable pageable) {
        return sanitize(pageable, ANOMALY_SORT_MAPPING);
    }

    public Pageable sanitizeSupervisorPageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size, Sort.unsorted());
    }

    private Pageable sanitize(Pageable pageable, Map<String, String> sortMapping) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size, translateSort(pageable.getSort(), sortMapping));
    }

    private Sort translateSort(Sort sort, Map<String, String> sortMapping) {
        if (sort.isUnsorted()) {
            return Sort.unsorted();
        }

        var orders = sort.stream()
                .filter(order -> sortMapping.containsKey(order.getProperty()))
                .map(order -> new Sort.Order(order.getDirection(), sortMapping.get(order.getProperty())))
                .toList();

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
