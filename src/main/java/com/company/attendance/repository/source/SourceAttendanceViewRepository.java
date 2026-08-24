package com.company.attendance.repository.source;

import com.company.attendance.entity.source.HrAnomalyView;
import com.company.attendance.entity.source.HrAnomalyViewId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface SourceAttendanceViewRepository extends JpaRepository<HrAnomalyView, HrAnomalyViewId> {

    List<HrAnomalyView> findByUpdatedAtAfterOrderByUpdatedAtAsc(LocalDateTime updatedAt);

    Slice<HrAnomalyView> findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(LocalDateTime updatedAt, Pageable pageable);

    Slice<HrAnomalyView> findByUpdatedAtAfterAndUpdatedAtLessThanEqualOrderByUpdatedAtAsc(
            LocalDateTime updatedAfter,
            LocalDateTime updatedBeforeOrEqual,
            Pageable pageable
    );
}
