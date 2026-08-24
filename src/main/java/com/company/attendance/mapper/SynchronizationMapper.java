package com.company.attendance.mapper;

import com.company.attendance.dto.AttendanceRecordDTO;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.entity.source.HrAnomalyView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SynchronizationMapper {

    private static final DateTimeFormatter SOURCE_RECORD_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public String generateSourceRecordId(HrAnomalyView source) {
        LocalDate dtPointage = source.getDtPointage();
        return source.getMatricule() + "_" + dtPointage.format(SOURCE_RECORD_DATE_FORMAT);
    }

    public AttendanceRecord toNewAttendanceRecord(HrAnomalyView source, LocalDateTime synchronizedAt) {
        AttendanceRecord record = AttendanceRecord.builder()
                .sourceRecordId(generateSourceRecordId(source))
                .synchronizedAt(synchronizedAt)
                .build();

        updateAttendanceRecord(record, source, synchronizedAt);
        return record;
    }

    public void updateAttendanceRecord(
            AttendanceRecord target,
            HrAnomalyView source,
            LocalDateTime synchronizedAt
    ) {
        target.setMatricule(trimToFallback(source.getMatricule(), "UNKNOWN"));
        target.setNom(trimToFallback(source.getNom(), source.getMatricule()));
        target.setSuperieur(trimToNull(source.getSuperieur()));
        target.setDtPointage(source.getDtPointage());
        target.setHierarchie(toInteger(source.getHierarchie()));
        target.setTypeJour(trimToNull(source.getTypeJour()));
        target.setPlanning(trimToNull(source.getPlanning()));
        target.setE1(source.getE1());
        target.setS1(source.getS1());
        target.setE2(source.getE2());
        target.setS2(source.getS2());
        target.setE3(source.getE3());
        target.setS3(source.getS3());
        target.setNbjTmoi(toBigDecimal(source.getNbjTmoi()));
        target.setNbhTmod(toBigDecimal(source.getNbhTmod()));
        target.setTypeAbsense(trimToNull(source.getTypeAbsense()));
        target.setDebutHsup(source.getDebutHsup());
        target.setImpair(toBoolean(source.getImpair()));
        target.setAnnee(toIntegerOrFallback(source.getAnnee(), source.getDtPointage().getYear()));
        target.setMois(toIntegerOrFallback(source.getMois(), source.getDtPointage().getMonthValue()));
        target.setDepartement(trimToNull(source.getDepartement()));
        target.setUpdatedAtSource(source.getUpdatedAt());
        target.setSynchronizedAt(synchronizedAt);
    }

    public AttendanceRecordDTO toDto(AttendanceRecord record) {
        return new AttendanceRecordDTO(
                record.getId(),
                record.getSourceRecordId(),
                record.getMatricule(),
                record.getNom(),
                record.getSuperieur(),
                record.getDtPointage(),
                record.getHierarchie(),
                record.getTypeJour(),
                record.getPlanning(),
                record.getE1(),
                record.getS1(),
                record.getE2(),
                record.getS2(),
                record.getE3(),
                record.getS3(),
                record.getNbjTmoi(),
                record.getNbhTmod(),
                record.getTypeAbsense(),
                record.getDebutHsup(),
                record.getImpair(),
                record.getAnnee(),
                record.getMois(),
                record.getDepartement(),
                record.getUpdatedAtSource(),
                record.getSynchronizedAt()
        );
    }

    private String trimToFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private Integer toInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim()).intValue();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric source value: " + value, ex);
        }
    }

    private Integer toInteger(Double value) {
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    private Integer toIntegerOrFallback(Double value, int fallback) {
        Integer converted = toInteger(value);
        return converted == null ? fallback : converted;
    }

    private Boolean toBoolean(Double value) {
        return value != null && value.intValue() == 1;
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }
}
