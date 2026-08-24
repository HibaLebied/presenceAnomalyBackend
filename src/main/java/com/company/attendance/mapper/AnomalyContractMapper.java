package com.company.attendance.mapper;

import com.company.attendance.dto.AnomalyResponse;
import com.company.attendance.entity.dashboard.Anomaly;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class AnomalyContractMapper {

    private final Clock clock;

    public AnomalyContractMapper() {
        this.clock = Clock.systemDefaultZone();
    }

    public AnomalyResponse toResponse(Anomaly anomaly) {
        AttendanceRecord record = anomaly.getAttendanceRecord();
        return new AnomalyResponse(
                anomaly.getId(),
                record.getMatricule(),
                record.getNom(),
                record.getSuperieur(),
                record.getDepartement(),
                record.getDtPointage(),
                record.getHierarchie(),
                hierarchyLabel(record.getHierarchie()),
                anomaly.getAnomalyType().getCode(),
                anomaly.getAnomalyType().getName(),
                anomaly.getStatus().name(),
                record.getE1(),
                record.getS1(),
                record.getE2(),
                record.getS2(),
                record.getE3(),
                record.getS3(),
                record.getSynchronizedAt(),
                delayDays(record.getDtPointage())
        );
    }

    private String hierarchyLabel(Integer hierarchyCode) {
        if (hierarchyCode == null) {
            return null;
        }
        return switch (hierarchyCode) {
            case 1 -> "MOI";
            case 2 -> "MOD";
            default -> "UNKNOWN";
        };
    }

    private long delayDays(LocalDate startDate) {
        if (startDate == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(startDate, LocalDate.now(clock));
    }
}
