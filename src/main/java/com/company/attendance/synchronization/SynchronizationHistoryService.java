package com.company.attendance.synchronization;

import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import com.company.attendance.exception.ResourceNotFoundException;
import com.company.attendance.repository.dashboard.SynchronizationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SynchronizationHistoryService {

    private static final Collection<SynchronizationStatus> SUCCESSFUL_STATUSES = java.util.List.of(
            SynchronizationStatus.SUCCESS,
            SynchronizationStatus.PARTIAL_SUCCESS
    );

    private final SynchronizationHistoryRepository synchronizationHistoryRepository;

    @Transactional(
            transactionManager = "dashboardTransactionManager",
            readOnly = true
    )
    public Optional<SynchronizationHistory> findLastSuccessfulSynchronization() {
        return synchronizationHistoryRepository.findTopByStatusInOrderByLastSyncTimeDesc(SUCCESSFUL_STATUSES);
    }

    @Transactional(
            transactionManager = "dashboardTransactionManager",
            readOnly = true
    )
    public Optional<SynchronizationHistory> findRunningSynchronization() {
        return synchronizationHistoryRepository.findTopByStatusOrderByStartTimeDesc(SynchronizationStatus.RUNNING);
    }

    @Transactional(
            transactionManager = "dashboardTransactionManager",
            readOnly = true
    )
    public SynchronizationHistory findById(Long synchronizationId) {
        return synchronizationHistoryRepository.findById(synchronizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Synchronization history not found: " + synchronizationId));
    }

    @Transactional(transactionManager = "dashboardTransactionManager")
    public void recoverRunningSynchronizations(LocalDateTime now) {
        List<SynchronizationHistory> staleSynchronizations =
                synchronizationHistoryRepository.findByStatus(SynchronizationStatus.RUNNING);

        for (SynchronizationHistory history : staleSynchronizations) {
            history.setEndTime(now);
            history.setStatus(resolveRecoveredStatus(history));
            history.setErrorMessage("Synchronization was marked as stale after application restart or timeout.");
        }

        synchronizationHistoryRepository.saveAll(staleSynchronizations);
    }

    @Transactional(transactionManager = "dashboardTransactionManager")
    public SynchronizationHistory startSynchronization(LocalDateTime startTime) {
        SynchronizationHistory history = SynchronizationHistory.builder()
                .startTime(startTime)
                .recordsInserted(0)
                .recordsUpdated(0)
                .status(SynchronizationStatus.RUNNING)
                .build();

        return synchronizationHistoryRepository.save(history);
    }

    @Transactional(transactionManager = "dashboardTransactionManager")
    public SynchronizationHistory markProgress(
            Long synchronizationId,
            LocalDateTime lastSyncTime,
            String lastSyncMatricule,
            LocalDateTime lastSyncDtPointage,
            int recordsInserted,
            int recordsUpdated
    ) {
        SynchronizationHistory history = synchronizationHistoryRepository.findById(synchronizationId)
                .orElseThrow(() -> new IllegalStateException("Synchronization history not found: " + synchronizationId));

        history.setLastSyncTime(lastSyncTime);
        history.setLastSyncMatricule(lastSyncMatricule);
        history.setLastSyncDtPointage(lastSyncDtPointage);
        history.setRecordsInserted(recordsInserted);
        history.setRecordsUpdated(recordsUpdated);

        return synchronizationHistoryRepository.save(history);
    }

    @Transactional(transactionManager = "dashboardTransactionManager")
    public SynchronizationHistory markSuccess(
            Long synchronizationId,
            LocalDateTime endTime,
            LocalDateTime lastSyncTime,
            String lastSyncMatricule,
            LocalDateTime lastSyncDtPointage,
            int recordsInserted,
            int recordsUpdated
    ) {
        SynchronizationHistory history = synchronizationHistoryRepository.findById(synchronizationId)
                .orElseThrow(() -> new IllegalStateException("Synchronization history not found: " + synchronizationId));

        history.setEndTime(endTime);
        history.setLastSyncTime(lastSyncTime);
        history.setLastSyncMatricule(lastSyncMatricule);
        history.setLastSyncDtPointage(lastSyncDtPointage);
        history.setRecordsInserted(recordsInserted);
        history.setRecordsUpdated(recordsUpdated);
        history.setStatus(SynchronizationStatus.SUCCESS);
        history.setErrorMessage(null);

        return synchronizationHistoryRepository.save(history);
    }

    @Transactional(transactionManager = "dashboardTransactionManager")
    public void markFailed(Long synchronizationId, LocalDateTime endTime, String errorMessage) {
        SynchronizationHistory history = synchronizationHistoryRepository.findById(synchronizationId)
                .orElseThrow(() -> new IllegalStateException("Synchronization history not found: " + synchronizationId));

        history.setEndTime(endTime);
        history.setStatus(SynchronizationStatus.FAILED);
        history.setErrorMessage(errorMessage);

        synchronizationHistoryRepository.save(history);
    }

    private SynchronizationStatus resolveRecoveredStatus(SynchronizationHistory history) {
        if (history.getLastSyncTime() == null) {
            return SynchronizationStatus.FAILED;
        }
        return SynchronizationStatus.PARTIAL_SUCCESS;
    }
}
