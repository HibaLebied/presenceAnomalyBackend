package com.company.attendance.repository.source;

import com.company.attendance.entity.source.HrAnomalyView;
import com.company.attendance.entity.source.HrAnomalyViewId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface SourceAttendanceViewRepository extends JpaRepository<HrAnomalyView, HrAnomalyViewId> {

    /**
     * Reads the source view with keyset pagination. The source timestamp alone is
     * not a unique cursor, so the view key completes the ordering and prevents
     * records sharing the same updated_at value from being skipped.
     */
    @org.springframework.data.jpa.repository.Query("""
            select h
            from HrAnomalyView h
            where h.updatedAt <= :upperBound
              and (
                    (
                        :lastUpdatedAt is null
                        and (:lowerBound is null or h.updatedAt > :lowerBound)
                    )
                    or (
                        :lastUpdatedAt is not null
                        and (
                            h.updatedAt > :lastUpdatedAt
                            or (
                                h.updatedAt = :lastUpdatedAt
                                and (
                                    h.id.matricule > :lastMatricule
                                    or (
                                        h.id.matricule = :lastMatricule
                                        and h.id.dtPointage > :lastDtPointage
                                    )
                                )
                            )
                        )
                    )
              )
            order by h.updatedAt asc, h.id.matricule asc, h.id.dtPointage asc
            """)
    List<HrAnomalyView> findNextBatch(
            LocalDateTime lowerBound,
            LocalDateTime upperBound,
            LocalDateTime lastUpdatedAt,
            String lastMatricule,
            LocalDateTime lastDtPointage,
            Pageable pageable
    );
}
