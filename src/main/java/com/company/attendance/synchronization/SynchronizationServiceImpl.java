package com.company.attendance.synchronization;

import com.company.attendance.dto.SynchronizationStatusDTO;
import com.company.attendance.detection.service.AttendanceAnomalyDetectionService;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import com.company.attendance.entity.source.HrAnomalyView;
import com.company.attendance.exception.SynchronizationAlreadyRunningException;
import com.company.attendance.exception.SynchronizationException;
import com.company.attendance.mapper.SynchronizationMapper;
import com.company.attendance.repository.dashboard.AttendanceRecordRepository;
import com.company.attendance.repository.source.SourceAttendanceViewRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SynchronizationServiceImpl implements SynchronizationService {

    private final SourceAttendanceViewRepository sourceAttendanceViewRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SynchronizationHistoryService synchronizationHistoryService;
    private final SynchronizationMapper synchronizationMapper;
    private final AttendanceAnomalyDetectionService attendanceAnomalyDetectionService;
    private final PlatformTransactionManager dashboardTransactionManager;
    private final Executor synchronizationTaskExecutor;
    private final AtomicBoolean synchronizationInProgress = new AtomicBoolean(false);
    private final int batchSize;

    public SynchronizationServiceImpl(
            SourceAttendanceViewRepository sourceAttendanceViewRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            SynchronizationHistoryService synchronizationHistoryService,
            SynchronizationMapper synchronizationMapper,
            AttendanceAnomalyDetectionService attendanceAnomalyDetectionService,
            @Qualifier("dashboardTransactionManager") PlatformTransactionManager dashboardTransactionManager,
            @Qualifier("synchronizationTaskExecutor") Executor synchronizationTaskExecutor,
            @Value("${attendance.synchronization.batch-size:500}") int batchSize
    ) {
        this.sourceAttendanceViewRepository = sourceAttendanceViewRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.synchronizationHistoryService = synchronizationHistoryService;
        this.synchronizationMapper = synchronizationMapper;
        this.attendanceAnomalyDetectionService = attendanceAnomalyDetectionService;
        this.dashboardTransactionManager = dashboardTransactionManager;
        this.synchronizationTaskExecutor = synchronizationTaskExecutor;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public SynchronizationStatusDTO startSynchronization() {
        if (!synchronizationInProgress.compareAndSet(false, true)) {
            throw new SynchronizationAlreadyRunningException(
                    "A synchronization is already running in this application instance"
            );
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            synchronizationHistoryService.findRunningSynchronization()
                    .ifPresent(running -> {
                        synchronizationInProgress.set(false);
                        throw new SynchronizationAlreadyRunningException(
                                "A synchronization is already running with id: " + running.getId()
                        );
                    });

            SynchronizationHistory history = synchronizationHistoryService.startSynchronization(startTime);
            try {
                synchronizationTaskExecutor.execute(() -> runSynchronizationJob(history.getId(), startTime));
            } catch (TaskRejectedException exception) {
                synchronizationHistoryService.markFailed(
                        history.getId(),
                        LocalDateTime.now(),
                        "Synchronization task executor rejected the job"
                );
                synchronizationInProgress.set(false);
                throw new SynchronizationException("Unable to start attendance synchronization", exception);
            }

            return toStatusDTO(history);
        } catch (RuntimeException exception) {
            synchronizationInProgress.set(false);
            throw exception;
        }
    }

    @Override
    public SynchronizationStatusDTO getSynchronizationStatus(Long synchronizationId) {
        return toStatusDTO(synchronizationHistoryService.findById(synchronizationId));
    }

    private void runSynchronizationJob(Long synchronizationId, LocalDateTime startTime) {
        log.info("Attendance synchronization {} started with batch size {}", synchronizationId, batchSize);
        try {
            SynchronizationHistory previousSynchronization = synchronizationHistoryService
                    .findLastSuccessfulSynchronization()
                    .orElse(null);

            LocalDateTime previousLastSyncTime = previousSynchronization == null
                    ? null
                    : previousSynchronization.getLastSyncTime();

            SourceCursor initialCursor = SourceCursor.from(previousSynchronization);

            SynchronizationExecution execution = synchronizeInBatches(
                    synchronizationId,
                    previousLastSyncTime,
                    startTime,
                    initialCursor
            );

            LocalDateTime endTime = LocalDateTime.now();
            synchronizationHistoryService.markSuccess(
                    synchronizationId,
                    endTime,
                    execution.lastSyncTime(),
                    execution.cursor().matricule(),
                    execution.cursor().dtPointage(),
                    execution.recordsInserted(),
                    execution.recordsUpdated()
            );

            log.info(
                    "Attendance synchronization {} completed. read={}, inserted={}, updated={}",
                    synchronizationId,
                    execution.recordsRead(),
                    execution.recordsInserted(),
                    execution.recordsUpdated()
            );
        } catch (Exception exception) {
            synchronizationHistoryService.markFailed(
                    synchronizationId,
                    LocalDateTime.now(),
                    exception.getMessage()
            );
            log.error("Attendance synchronization {} failed", synchronizationId, exception);
        } finally {
            synchronizationInProgress.set(false);
        }
    }

    private SynchronizationExecution synchronizeInBatches(
            Long synchronizationId,
            LocalDateTime previousLastSyncTime,
            LocalDateTime synchronizationBoundary,
            SourceCursor initialCursor
    ) {
        int pageNumber = 0;
        int totalRead = 0;
        int totalInserted = 0;
        int totalUpdated = 0;
        LocalDateTime lastSyncTime = previousLastSyncTime;
        SourceCursor cursor = initialCursor;

        List<HrAnomalyView> sourceBatch;
        do {
            sourceBatch = loadSourceRecordsBatch(
                    previousLastSyncTime,
                    synchronizationBoundary,
                    cursor
            );

            if (!sourceBatch.isEmpty()) {
                SynchronizationExecution batchExecution = synchronizeBatch(sourceBatch);
                totalRead += batchExecution.recordsRead();
                totalInserted += batchExecution.recordsInserted();
                totalUpdated += batchExecution.recordsUpdated();
                lastSyncTime = batchExecution.lastSyncTime();
                cursor = SourceCursor.from(sourceBatch.get(sourceBatch.size() - 1));
                synchronizationHistoryService.markProgress(
                        synchronizationId,
                        lastSyncTime,
                        cursor.matricule(),
                        cursor.dtPointage(),
                        totalInserted,
                        totalUpdated
                );
                log.info(
                        "Attendance synchronization {} batch {} completed. batchSize={}, totalInserted={}, totalUpdated={}, lastSyncTime={}",
                        synchronizationId,
                        pageNumber + 1,
                        batchExecution.recordsRead(),
                        totalInserted,
                        totalUpdated,
                        lastSyncTime
                );
            }

            pageNumber++;
        } while (sourceBatch.size() == batchSize);

        return new SynchronizationExecution(
                totalRead,
                totalInserted,
                totalUpdated,
                lastSyncTime,
                cursor
        );
    }

    private List<HrAnomalyView> loadSourceRecordsBatch(
            LocalDateTime previousLastSyncTime,
            LocalDateTime synchronizationBoundary,
            SourceCursor cursor
    ) {
        return sourceAttendanceViewRepository.findNextBatch(
            previousLastSyncTime,
            synchronizationBoundary,
            cursor.updatedAt(),
            cursor.matricule(),
            cursor.dtPointage(),
            PageRequest.of(0, batchSize)
        );
    }

    private SynchronizationExecution synchronizeBatch(List<HrAnomalyView> sourceRecords) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(dashboardTransactionManager);

        return transactionTemplate.execute(status -> {
            AtomicInteger inserted = new AtomicInteger();
            AtomicInteger updated = new AtomicInteger();

            Map<String, AttendanceRecord> existingRecordsBySourceId = loadExistingRecordsBySourceId(sourceRecords);
            LocalDateTime synchronizedAt = LocalDateTime.now();

            List<AttendanceRecord> synchronizedRecords = sourceRecords.stream()
                    .map(sourceRecord -> synchronizeRecord(
                            sourceRecord,
                            existingRecordsBySourceId,
                            synchronizedAt,
                            inserted,
                            updated
                    ))
                    .toList();

            List<AttendanceRecord> savedRecords = attendanceRecordRepository.saveAll(synchronizedRecords);
            attendanceAnomalyDetectionService.detectForRecords(savedRecords);

            return new SynchronizationExecution(
                    sourceRecords.size(),
                    inserted.get(),
                    updated.get(),
                    resolveBatchLastSyncTime(sourceRecords),
                    SourceCursor.empty()
            );
        });
    }

    private Map<String, AttendanceRecord> loadExistingRecordsBySourceId(List<HrAnomalyView> sourceRecords) {
        List<String> sourceRecordIds = sourceRecords.stream()
                .map(synchronizationMapper::generateSourceRecordId)
                .toList();

        return attendanceRecordRepository.findBySourceRecordIdIn(sourceRecordIds)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getSourceRecordId, Function.identity()));
    }

    private AttendanceRecord synchronizeRecord(
            HrAnomalyView sourceRecord,
            Map<String, AttendanceRecord> existingRecordsBySourceId,
            LocalDateTime synchronizedAt,
            AtomicInteger inserted,
            AtomicInteger updated
    ) {
        String sourceRecordId = synchronizationMapper.generateSourceRecordId(sourceRecord);

        AttendanceRecord existingRecord = existingRecordsBySourceId.get(sourceRecordId);
        if (existingRecord != null) {
            synchronizationMapper.updateAttendanceRecord(existingRecord, sourceRecord, synchronizedAt);
            updated.incrementAndGet();
            return existingRecord;
        }

        inserted.incrementAndGet();
        return synchronizationMapper.toNewAttendanceRecord(sourceRecord, synchronizedAt);
    }

    private LocalDateTime resolveBatchLastSyncTime(List<HrAnomalyView> sourceRecords) {
        return sourceRecords.get(sourceRecords.size() - 1).getUpdatedAt();
    }

    private SynchronizationStatusDTO toStatusDTO(SynchronizationHistory history) {
        int recordsInserted = history.getRecordsInserted() == null ? 0 : history.getRecordsInserted();
        int recordsUpdated = history.getRecordsUpdated() == null ? 0 : history.getRecordsUpdated();

        return new SynchronizationStatusDTO(
                history.getId(),
                history.getStatus(),
                history.getStartTime(),
                history.getEndTime(),
                history.getLastSyncTime(),
                recordsInserted,
                recordsUpdated,
                recordsInserted + recordsUpdated,
                history.getErrorMessage()
        );
    }

    private record SynchronizationExecution(
            int recordsRead,
            int recordsInserted,
            int recordsUpdated,
            LocalDateTime lastSyncTime,
            SourceCursor cursor
    ) {
    }

    private record SourceCursor(
            LocalDateTime updatedAt,
            String matricule,
            LocalDateTime dtPointage
    ) {
        private static SourceCursor empty() {
            return new SourceCursor(null, null, null);
        }

        private static SourceCursor from(SynchronizationHistory history) {
            if (history == null
                    || history.getStatus() != SynchronizationStatus.PARTIAL_SUCCESS
                    || history.getLastSyncTime() == null
                    || history.getLastSyncMatricule() == null
                    || history.getLastSyncDtPointage() == null) {
                return empty();
            }

            return new SourceCursor(
                    history.getLastSyncTime(),
                    history.getLastSyncMatricule(),
                    history.getLastSyncDtPointage()
            );
        }

        private static SourceCursor from(HrAnomalyView sourceRecord) {
            return new SourceCursor(
                    sourceRecord.getUpdatedAt(),
                    sourceRecord.getMatricule(),
                    sourceRecord.getDtPointageDateTime()
            );
        }
    }
}
