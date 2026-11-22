package com.company.attendance.detection.support;

import com.company.attendance.entity.dashboard.AttendanceRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AttendanceRecordTestBuilder {

    private AttendanceRecordTestBuilder() {
    }

    public static Builder record() {
        return new Builder();
    }

    public static final class Builder {

        private final AttendanceRecord record;

        private Builder() {
            this.record = AttendanceRecord.builder()
                    .sourceRecordId("M001_20261120")
                    .matricule("M001")
                    .nom("Employe Test")
                    .superieur("Superviseur Test")
                    .dtPointage(LocalDate.of(2026, 11, 20))
                    .hierarchie(2)
                    .typeJour("T")
                    .planning("SH3")
                    .nbjTmoi(BigDecimal.ONE)
                    .nbhTmod(BigDecimal.ONE)
                    .impair(false)
                    .annee(2026)
                    .mois(11)
                    .departement("Production")
                    .updatedAtSource(LocalDateTime.of(2026, 11, 20, 18, 0))
                    .synchronizedAt(LocalDateTime.of(2026, 11, 20, 18, 1))
                    .build();
        }

        public Builder moi() {
            record.setHierarchie(1);
            return this;
        }

        public Builder mod() {
            record.setHierarchie(2);
            return this;
        }

        public Builder typeJour(String typeJour) {
            record.setTypeJour(typeJour);
            return this;
        }

        public Builder planning(String planning) {
            record.setPlanning(planning);
            return this;
        }

        public Builder date(LocalDate date) {
            record.setDtPointage(date);
            return this;
        }

        public Builder punches(Integer firstEntry, Integer firstExit) {
            record.setE1(firstEntry);
            record.setS1(firstExit);
            return this;
        }

        public Builder secondPunches(Integer secondEntry, Integer secondExit) {
            record.setE2(secondEntry);
            record.setS2(secondExit);
            return this;
        }

        public Builder thirdPunches(Integer thirdEntry, Integer thirdExit) {
            record.setE3(thirdEntry);
            record.setS3(thirdExit);
            return this;
        }

        public Builder noPunches() {
            record.setE1(null);
            record.setS1(null);
            record.setE2(null);
            record.setS2(null);
            record.setE3(null);
            record.setS3(null);
            return this;
        }

        public Builder absenceType(String absenceType) {
            record.setTypeAbsense(absenceType);
            return this;
        }

        public Builder impair(boolean impair) {
            record.setImpair(impair);
            return this;
        }

        public Builder nbjTmoi(String value) {
            record.setNbjTmoi(value == null ? null : new BigDecimal(value));
            return this;
        }

        public Builder nbhTmod(String value) {
            record.setNbhTmod(value == null ? null : new BigDecimal(value));
            return this;
        }

        public Builder debutHsup(Integer debutHsup) {
            record.setDebutHsup(debutHsup);
            return this;
        }

        public AttendanceRecord build() {
            return record;
        }
    }
}
