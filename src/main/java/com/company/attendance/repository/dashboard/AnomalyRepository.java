package com.company.attendance.repository.dashboard;

import com.company.attendance.dto.DelayCategoryCounts;
import com.company.attendance.dto.DepartmentStatsResponse;
import com.company.attendance.dto.SupervisorSummaryResponse;
import com.company.attendance.entity.dashboard.Anomaly;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    Optional<Anomaly> findByAttendanceRecord(AttendanceRecord attendanceRecord);

    boolean existsByAttendanceRecord(AttendanceRecord attendanceRecord);

    void deleteByAttendanceRecord(AttendanceRecord attendanceRecord);

    @Query("""
            select count(a)
            from Anomaly a
            where a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            """)
    long countFiltered(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy
    );

    @Query("""
            select new com.company.attendance.dto.DepartmentStatsResponse(a.attendanceRecord.departement, count(a))
            from Anomaly a
            where a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            group by a.attendanceRecord.departement
            order by count(a) desc, a.attendanceRecord.departement asc
            """)
    List<DepartmentStatsResponse> findDepartmentStatistics(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy
    );

    @Query(
            value = """
                    select new com.company.attendance.dto.SupervisorSummaryResponse(a.attendanceRecord.superieur, count(a))
                    from Anomaly a
                    where a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
                      and a.attendanceRecord.superieur is not null
                      and trim(a.attendanceRecord.superieur) <> ''
                      and (:department is null or a.attendanceRecord.departement = :department)
                      and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
                      and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
                      and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
                    group by a.attendanceRecord.superieur
                    order by count(a) desc, a.attendanceRecord.superieur asc
                    """,
            countQuery = """
                    select count(distinct a.attendanceRecord.superieur)
                    from Anomaly a
                    where a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
                      and a.attendanceRecord.superieur is not null
                      and trim(a.attendanceRecord.superieur) <> ''
                      and (:department is null or a.attendanceRecord.departement = :department)
                      and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
                      and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
                      and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
                    """
    )
    Page<SupervisorSummaryResponse> findSupervisorRanking(
            @Param("department") String department,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy,
            Pageable pageable
    );

    @Query("""
            select new com.company.attendance.dto.SupervisorSummaryResponse(a.attendanceRecord.superieur, count(a))
            from Anomaly a
            where a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and a.attendanceRecord.superieur is not null
              and trim(a.attendanceRecord.superieur) <> ''
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            group by a.attendanceRecord.superieur
            order by count(a) desc, a.attendanceRecord.superieur asc
            """)
    List<SupervisorSummaryResponse> findSupervisorRankingSummary(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy,
            Pageable pageable
    );

    @Query("""
            select count(a)
            from Anomaly a
            where a.attendanceRecord.superieur = :supervisorId
              and a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            """)
    long countBySupervisorId(
            @Param("supervisorId") String supervisorId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy
    );

    @Query("""
            select distinct a.attendanceRecord.departement
            from Anomaly a
            where a.attendanceRecord.superieur = :supervisorId
              and a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            order by a.attendanceRecord.departement asc
            """)
    List<String> findDepartmentsBySupervisorId(
            @Param("supervisorId") String supervisorId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy
    );

    @Query("""
            select new com.company.attendance.dto.DelayCategoryCounts(
                sum(case when a.attendanceRecord.dtPointage = :oneDayDate then 1 else 0 end),
                sum(case when a.attendanceRecord.dtPointage between :twoToNineStartDate and :twoToNineEndDate then 1 else 0 end),
                sum(case when a.attendanceRecord.dtPointage <= :tenPlusDate then 1 else 0 end)
            )
            from Anomaly a
            where a.attendanceRecord.superieur = :supervisorId
              and a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE
              and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
              and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
              and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
            """)
    DelayCategoryCounts countDelayCategoriesBySupervisorId(
            @Param("supervisorId") String supervisorId,
            @Param("oneDayDate") LocalDate oneDayDate,
            @Param("twoToNineStartDate") LocalDate twoToNineStartDate,
            @Param("twoToNineEndDate") LocalDate twoToNineEndDate,
            @Param("tenPlusDate") LocalDate tenPlusDate,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy
    );

    @Query(
            value = """
                    select a
                    from Anomaly a
                    join fetch a.attendanceRecord
                    join fetch a.anomalyType
                    where (:includeIgnored = true or a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE)
                      and (:employeeId is null or lower(a.attendanceRecord.matricule) like lower(concat('%', :employeeId, '%')))
                      and (:employeeName is null or lower(a.attendanceRecord.nom) like lower(concat('%', :employeeName, '%')))
                      and (:department is null or a.attendanceRecord.departement = :department)
                      and (:supervisorId is null or a.attendanceRecord.superieur = :supervisorId)
                      and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
                      and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
                      and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
                    """,
            countQuery = """
                    select count(a)
                    from Anomaly a
                    where (:includeIgnored = true or a.status = com.company.attendance.entity.dashboard.AnomalyStatus.ACTIVE)
                      and (:employeeId is null or lower(a.attendanceRecord.matricule) like lower(concat('%', :employeeId, '%')))
                      and (:employeeName is null or lower(a.attendanceRecord.nom) like lower(concat('%', :employeeName, '%')))
                      and (:department is null or a.attendanceRecord.departement = :department)
                      and (:supervisorId is null or a.attendanceRecord.superieur = :supervisorId)
                      and (:periodStart is null or a.attendanceRecord.dtPointage >= :periodStart)
                      and (:periodEnd is null or a.attendanceRecord.dtPointage <= :periodEnd)
                      and (:hierarchy is null or a.attendanceRecord.hierarchie = :hierarchy)
                    """
    )
    Page<Anomaly> searchAnomalies(
            @Param("employeeId") String employeeId,
            @Param("employeeName") String employeeName,
            @Param("department") String department,
            @Param("supervisorId") String supervisorId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("hierarchy") Integer hierarchy,
            @Param("includeIgnored") boolean includeIgnored,
            Pageable pageable
    );
}
