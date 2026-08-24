package com.company.attendance.repository.dashboard;

import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findBySourceRecordId(String sourceRecordId);

    Optional<AttendanceRecord> findByMatriculeAndDtPointage(String matricule, LocalDate dtPointage);

    boolean existsBySourceRecordId(String sourceRecordId);

    List<AttendanceRecord> findBySourceRecordIdIn(Collection<String> sourceRecordIds);

    @Query("""
            select distinct ar.annee
            from AttendanceRecord ar
            where ar.annee is not null
            order by ar.annee desc
            """)
    List<Integer> findAvailableYears();
}
