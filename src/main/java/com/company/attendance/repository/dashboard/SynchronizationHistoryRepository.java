package com.company.attendance.repository.dashboard;

import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SynchronizationHistoryRepository extends JpaRepository<SynchronizationHistory, Long> {

    Optional<SynchronizationHistory> findTopByStatusOrderByStartTimeDesc(SynchronizationStatus status);

    Optional<SynchronizationHistory> findTopByStatusOrderByEndTimeDesc(SynchronizationStatus status);

    Optional<SynchronizationHistory> findTopByStatusInOrderByLastSyncTimeDesc(Collection<SynchronizationStatus> statuses);

    List<SynchronizationHistory> findByStatus(SynchronizationStatus status);
}
