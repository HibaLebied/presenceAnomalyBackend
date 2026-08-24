package com.company.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecordDTO(
        Long id,
        String sourceRecordId,
        String matricule,
        String nom,
        String superieur,
        LocalDate dtPointage,
        Integer hierarchie,
        String typeJour,
        String planning,
        Integer e1,
        Integer s1,
        Integer e2,
        Integer s2,
        Integer e3,
        Integer s3,
        BigDecimal nbjTmoi,
        BigDecimal nbhTmod,
        String typeAbsense,
        Integer debutHsup,
        Boolean impair,
        Integer annee,
        Integer mois,
        String departement,
        LocalDateTime updatedAtSource,
        LocalDateTime synchronizedAt
) {
}
