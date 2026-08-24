package com.company.attendance.synchronization;

import com.company.attendance.dto.SynchronizationStatusDTO;
import com.company.attendance.detection.service.AttendanceAnomalyDetectionService;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.source.HrAnomalyView;
import com.company.attendance.exception.SynchronizationAlreadyRunningException;
import com.company.attendance.exception.SynchronizationException;
import com.company.attendance.mapper.SynchronizationMapper;
import com.company.attendance.repository.dashboard.AttendanceRecordRepository;
import com.company.attendance.repository.source.SourceAttendanceViewRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
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
        LocalDateTime startTime = LocalDateTime.now();
        synchronizationHistoryService.findRunningSynchronization()
                .ifPresent(running -> {
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
            throw new SynchronizationException("Unable to start attendance synchronization", exception);
        }

        return toStatusDTO(history);
    }

    @Override
    public SynchronizationStatusDTO getSynchronizationStatus(Long synchronizationId) {
        return toStatusDTO(synchronizationHistoryService.findById(synchronizationId));
    }

    private void runSynchronizationJob(Long synchronizationId, LocalDateTime startTime) {
        log.info("Attendance synchronization {} started with batch size {}", synchronizationId, batchSize);
        try {
            LocalDateTime previousLastSyncTime = synchronizationHistoryService.findLastSuccessfulSynchronization()
                    .map(SynchronizationHistory::getLastSyncTime)
                    .orElse(null);

            SynchronizationExecution execution = synchronizeInBatches(synchronizationId, previousLastSyncTime, startTime);

            LocalDateTime endTime = LocalDateTime.now();
            synchronizationHistoryService.markSuccess(
                    synchronizationId,
                    endTime,
                    execution.lastSyncTime(),
                    execution.recordsInserted(),
                    execution.recordsUpdated()
            );

            log.info(
                    "Attendance synchronization {} completed. inserted={}, updated={}",
                    synchronizationId,
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
        }
    }

    private SynchronizationExecution synchronizeInBatches(
            Long synchronizationId,
            LocalDateTime previousLastSyncTime,
            LocalDateTime synchronizationBoundary
    ) {
        int pageNumber = 0;
        int totalInserted = 0;
        int totalUpdated = 0;
        LocalDateTime lastSyncTime = previousLastSyncTime;

        Slice<HrAnomalyView> sourcePage;
        do {
            sourcePage = loadSourceRecordsBatch(previousLastSyncTime, synchronizationBoundary, pageNumber);

            if (!sourcePage.isEmpty()) {
                SynchronizationExecution batchExecution = synchronizeBatch(sourcePage.getContent());
                totalInserted += batchExecution.recordsInserted();
                totalUpdated += batchExecution.recordsUpdated();
                lastSyncTime = batchExecution.lastSyncTime();
                synchronizationHistoryService.markProgress(
                        synchronizationId,
                        lastSyncTime,
                        totalInserted,
                        totalUpdated
                );
                log.info(
                        "Attendance synchronization {} batch {} completed. batchSize={}, totalInserted={}, totalUpdated={}, lastSyncTime={}",
                        synchronizationId,
                        pageNumber + 1,
                        sourcePage.getNumberOfElements(),
                        totalInserted,
                        totalUpdated,
                        lastSyncTime
                );
            }

            pageNumber++;
        } while (sourcePage.hasNext());

        return new SynchronizationExecution(
                totalInserted,
                totalUpdated,
                lastSyncTime
        );
    }

    private Slice<HrAnomalyView> loadSourceRecordsBatch(
            LocalDateTime previousLastSyncTime,
            LocalDateTime synchronizationBoundary,
            int pageNumber
    ) {
        PageRequest pageRequest = PageRequest.of(pageNumber, batchSize);

        if (previousLastSyncTime == null) {
            return sourceAttendanceViewRepository.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(
                    synchronizationBoundary,
                    pageRequest
            );
        }

        return sourceAttendanceViewRepository.findByUpdatedAtAfterAndUpdatedAtLessThanEqualOrderByUpdatedAtAsc(
                previousLastSyncTime,
                synchronizationBoundary,
                pageRequest
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
                    inserted.get(),
                    updated.get(),
                    resolveBatchLastSyncTime(sourceRecords)
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
            int recordsInserted,
            int recordsUpdated,
            LocalDateTime lastSyncTime
    ) {
    }
}
