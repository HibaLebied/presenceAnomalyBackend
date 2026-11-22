package com.company.attendance.synchronization;

import com.company.attendance.detection.service.AttendanceAnomalyDetectionService;
import com.company.attendance.dto.SynchronizationStatusDTO;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import com.company.attendance.entity.source.HrAnomalyView;
import com.company.attendance.entity.source.HrAnomalyViewId;
import com.company.attendance.exception.SynchronizationAlreadyRunningException;
import com.company.attendance.exception.SynchronizationException;
import com.company.attendance.mapper.SynchronizationMapper;
import com.company.attendance.repository.dashboard.AttendanceRecordRepository;
import com.company.attendance.repository.source.SourceAttendanceViewRepository;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SynchronizationServiceImplTest {

    private static final LocalDateTime FIRST_UPDATED_AT = LocalDateTime.of(2026, 11, 20, 18, 0);

    @Test
    void shouldInsertAllRecordsDuringFirstSynchronization() {
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(
                sourceRecord("M001", FIRST_UPDATED_AT, "Employe 1"),
                sourceRecord("M002", FIRST_UPDATED_AT.plusSeconds(1), "Employe 2")
        ));
        source.addBatch(List.of());
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();
        HistoryDouble history = new HistoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();
        assertEquals(SynchronizationStatus.SUCCESS, result.status());
        assertEquals(2, result.recordsInserted());
        assertEquals(0, result.recordsUpdated());
        assertEquals(2, dashboard.savedRecords.size());
        assertEquals(1, history.progressCalls);
        assertNull(source.calls.get(0).lowerBound());
        assertNull(source.calls.get(0).lastUpdatedAt());
        assertNotNull(source.calls.get(0).upperBound());
    }

    @Test
    void shouldReadOnlyRecordsAfterThePreviousSuccessfulSync() {
        LocalDateTime previousLastSync = FIRST_UPDATED_AT;
        HistoryDouble history = new HistoryDouble();
        history.lastSuccessful = Optional.of(history(
                SynchronizationStatus.SUCCESS,
                previousLastSync,
                null,
                null
        ));

        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(sourceRecord("M003", previousLastSync.plusSeconds(10), "Employe 3")));
        source.addBatch(List.of());
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();

        assertEquals(SynchronizationStatus.SUCCESS, result.status());
        assertEquals(1, result.totalProcessed());
        assertEquals(previousLastSync, source.calls.get(0).lowerBound());
        assertNull(source.calls.get(0).lastUpdatedAt());
    }

    @Test
    void shouldResumePartialSynchronizationFromTheCompleteCursor() {
        LocalDateTime cursorTime = FIRST_UPDATED_AT;
        LocalDateTime cursorDate = LocalDateTime.of(2026, 11, 20, 0, 0);
        HistoryDouble history = new HistoryDouble();
        history.lastSuccessful = Optional.of(history(
                SynchronizationStatus.PARTIAL_SUCCESS,
                cursorTime,
                "M001",
                cursorDate
        ));

        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(sourceRecord("M002", cursorTime, "Employe 2")));
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();

        assertEquals(SynchronizationStatus.SUCCESS, result.status());
        SourceCall call = source.calls.get(0);
        assertEquals(cursorTime, call.lowerBound());
        assertEquals(cursorTime, call.lastUpdatedAt());
        assertEquals("M001", call.lastMatricule());
        assertEquals(cursorDate, call.lastDtPointage());
    }

    @Test
    void shouldCompleteSuccessfullyWithoutWritingWhenSourceHasNoChanges() {
        HistoryDouble history = new HistoryDouble();
        history.lastSuccessful = Optional.of(history(
                SynchronizationStatus.SUCCESS,
                FIRST_UPDATED_AT,
                null,
                null
        ));
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of());
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();

        assertEquals(SynchronizationStatus.SUCCESS, result.status());
        assertEquals(0, result.totalProcessed());
        assertEquals(0, dashboard.saveAllCalls);
        assertEquals(0, history.progressCalls);
    }

    @Test
    void shouldUpdateExistingRecordInsteadOfCreatingADuplicate() {
        HrAnomalyView sourceRecord = sourceRecord("M001", FIRST_UPDATED_AT.plusDays(1), "Nom modifie");
        AttendanceRecord existing = AttendanceRecordTestBuilder.record()
                .date(LocalDate.of(2026, 11, 20))
                .build();
        existing.setSourceRecordId("M001_20261120");

        HistoryDouble history = new HistoryDouble();
        history.lastSuccessful = Optional.of(history(
                SynchronizationStatus.SUCCESS,
                FIRST_UPDATED_AT,
                null,
                null
        ));
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(sourceRecord));
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();
        dashboard.existingRecords = List.of(existing);

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();

        assertEquals(0, result.recordsInserted());
        assertEquals(1, result.recordsUpdated());
        assertEquals(1, dashboard.saveAllCalls);
        assertEquals(1, dashboard.savedRecords.size());
        assertEquals(existing, dashboard.savedRecords.get(0));
        assertEquals("Nom modifie", existing.getNom());
    }

    @Test
    void shouldRejectASecondSynchronizationWhileTheFirstIsRunning() {
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();
        HistoryDouble history = new HistoryDouble();
        AtomicReference<Runnable> queuedTask = new AtomicReference<>();
        SynchronizationServiceImpl service = newService(
                source,
                dashboard,
                history,
                records -> {
                },
                queuedTask::set,
                2
        );

        SynchronizationStatusDTO first = service.startSynchronization();

        assertEquals(SynchronizationStatus.RUNNING, first.status());
        assertNotNull(queuedTask.get());
        assertThrows(SynchronizationAlreadyRunningException.class, service::startSynchronization);

        source.addBatch(List.of());
        queuedTask.get().run();
        assertEquals(SynchronizationStatus.SUCCESS, history.current.getStatus());
    }

    @Test
    void shouldMarkSynchronizationFailedWhenTaskCannotBeSubmitted() {
        HistoryDouble history = new HistoryDouble();
        SynchronizationServiceImpl service = newService(
                new SourceRepositoryDouble(),
                new DashboardRepositoryDouble(),
                history,
                records -> {
                },
                task -> {
                    throw new TaskRejectedException("executor unavailable");
                },
                2
        );

        assertThrows(SynchronizationException.class, service::startSynchronization);
        assertEquals(SynchronizationStatus.FAILED, history.current.getStatus());
        assertEquals("Synchronization task executor rejected the job", history.current.getErrorMessage());
    }

    @Test
    void shouldMarkSynchronizationFailedWhenABatchThrows() {
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(sourceRecord("M001", FIRST_UPDATED_AT, "Employe 1")));
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();
        dashboard.failOnSaveAll = true;
        HistoryDouble history = new HistoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 2).startSynchronization();

        assertEquals(SynchronizationStatus.FAILED, result.status());
        assertNotNull(result.errorMessage());
        assertEquals(SynchronizationStatus.FAILED, history.current.getStatus());
    }

    @Test
    void shouldMarkPartialSuccessWhenACompletedBatchIsFollowedByAFailure() {
        SourceRepositoryDouble source = new SourceRepositoryDouble();
        source.addBatch(List.of(sourceRecord("M001", FIRST_UPDATED_AT, "Employe 1")));
        source.addBatch(List.of(sourceRecord("M002", FIRST_UPDATED_AT.plusSeconds(1), "Employe 2")));
        DashboardRepositoryDouble dashboard = new DashboardRepositoryDouble();
        dashboard.failAfterSuccessfulSaves = 1;
        HistoryDouble history = new HistoryDouble();

        SynchronizationStatusDTO result = newService(source, dashboard, history, records -> {
        }, Runnable::run, 1).startSynchronization();

        assertEquals(SynchronizationStatus.PARTIAL_SUCCESS, result.status());
        assertEquals(1, result.recordsInserted());
        assertEquals(1, result.totalProcessed());
        assertNotNull(result.errorMessage());
        assertEquals(SynchronizationStatus.PARTIAL_SUCCESS, history.current.getStatus());
    }

    private SynchronizationServiceImpl newService(
            SourceRepositoryDouble source,
            DashboardRepositoryDouble dashboard,
            HistoryDouble history,
            AttendanceAnomalyDetectionService anomalyDetectionService,
            java.util.concurrent.Executor executor,
            int batchSize
    ) {
        return new SynchronizationServiceImpl(
                source.proxy(),
                dashboard.proxy(),
                history,
                new SynchronizationMapper(),
                anomalyDetectionService,
                transactionManager(),
                executor,
                batchSize
        );
    }

    private static SynchronizationHistory history(
            SynchronizationStatus status,
            LocalDateTime lastSyncTime,
            String matricule,
            LocalDateTime dtPointage
    ) {
        return SynchronizationHistory.builder()
                .id(99L)
                .startTime(FIRST_UPDATED_AT.minusHours(1))
                .lastSyncTime(lastSyncTime)
                .lastSyncMatricule(matricule)
                .lastSyncDtPointage(dtPointage)
                .recordsInserted(1)
                .recordsUpdated(0)
                .status(status)
                .build();
    }

    private static HrAnomalyView sourceRecord(String matricule, LocalDateTime updatedAt, String name) {
        try {
            Constructor<HrAnomalyView> constructor = HrAnomalyView.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            HrAnomalyView source = constructor.newInstance();
            setField(source, "id", new HrAnomalyViewId(matricule, LocalDateTime.of(2026, 11, 20, 0, 0)));
            setField(source, "nom", name);
            setField(source, "superieur", "Superviseur Test");
            setField(source, "hierarchie", "2");
            setField(source, "typeJour", "T");
            setField(source, "planning", "SH3");
            setField(source, "nbjTmoi", 1D);
            setField(source, "nbhTmod", 1D);
            setField(source, "impair", 0D);
            setField(source, "annee", 2026D);
            setField(source, "mois", 11D);
            setField(source, "departement", "Production");
            setField(source, "updatedAt", updatedAt);
            return source;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to create source view test data", exception);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = HrAnomalyView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }

    private record SourceCall(
            LocalDateTime lowerBound,
            LocalDateTime upperBound,
            LocalDateTime lastUpdatedAt,
            String lastMatricule,
            LocalDateTime lastDtPointage,
            Pageable pageable
    ) {
    }

    private static final class SourceRepositoryDouble {
        private final Deque<List<HrAnomalyView>> batches = new ArrayDeque<>();
        private final List<SourceCall> calls = new ArrayList<>();

        private void addBatch(List<HrAnomalyView> batch) {
            batches.addLast(batch);
        }

        private SourceAttendanceViewRepository proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("findNextBatch")) {
                    calls.add(new SourceCall(
                            (LocalDateTime) args[0],
                            (LocalDateTime) args[1],
                            (LocalDateTime) args[2],
                            (String) args[3],
                            (LocalDateTime) args[4],
                            (Pageable) args[5]
                    ));
                    return batches.isEmpty() ? List.of() : batches.removeFirst();
                }
                return defaultValue(method.getReturnType());
            };
            return SynchronizationServiceImplTest.proxy(SourceAttendanceViewRepository.class, handler);
        }
    }

    private static final class DashboardRepositoryDouble {
        private List<AttendanceRecord> existingRecords = List.of();
        private List<AttendanceRecord> savedRecords = List.of();
        private int saveAllCalls;
        private boolean failOnSaveAll;
        private int failAfterSuccessfulSaves = -1;

        private AttendanceRecordRepository proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("findBySourceRecordIdIn")) {
                    Collection<String> sourceIds = (Collection<String>) args[0];
                    return existingRecords.stream()
                            .filter(record -> sourceIds.contains(record.getSourceRecordId()))
                            .toList();
                }
                if (method.getName().equals("saveAll")) {
                    if (failOnSaveAll || saveAllCalls >= failAfterSuccessfulSaves && failAfterSuccessfulSaves >= 0) {
                        throw new IllegalStateException("dashboard database unavailable");
                    }
                    saveAllCalls++;
                    savedRecords = new ArrayList<>((Collection<AttendanceRecord>) args[0]);
                    return savedRecords;
                }
                return defaultValue(method.getReturnType());
            };
            return SynchronizationServiceImplTest.proxy(AttendanceRecordRepository.class, handler);
        }
    }

    private static final class HistoryDouble extends SynchronizationHistoryService {
        private Optional<SynchronizationHistory> running = Optional.empty();
        private Optional<SynchronizationHistory> lastSuccessful = Optional.empty();
        private SynchronizationHistory current;
        private int progressCalls;

        private HistoryDouble() {
            super(null);
        }

        @Override
        public Optional<SynchronizationHistory> findRunningSynchronization() {
            return running;
        }

        @Override
        public Optional<SynchronizationHistory> findLastSuccessfulSynchronization() {
            return lastSuccessful;
        }

        @Override
        public SynchronizationHistory startSynchronization(LocalDateTime startTime) {
            current = SynchronizationHistory.builder()
                    .id(1L)
                    .startTime(startTime)
                    .recordsInserted(0)
                    .recordsUpdated(0)
                    .status(SynchronizationStatus.RUNNING)
                    .build();
            return current;
        }

        @Override
        public SynchronizationHistory markProgress(
                Long id,
                LocalDateTime lastSyncTime,
                String lastSyncMatricule,
                LocalDateTime lastSyncDtPointage,
                int recordsInserted,
                int recordsUpdated
        ) {
            progressCalls++;
            current.setLastSyncTime(lastSyncTime);
            current.setLastSyncMatricule(lastSyncMatricule);
            current.setLastSyncDtPointage(lastSyncDtPointage);
            current.setRecordsInserted(recordsInserted);
            current.setRecordsUpdated(recordsUpdated);
            return current;
        }

        @Override
        public SynchronizationHistory markSuccess(
                Long id,
                LocalDateTime endTime,
                LocalDateTime lastSyncTime,
                String lastSyncMatricule,
                LocalDateTime lastSyncDtPointage,
                int recordsInserted,
                int recordsUpdated
        ) {
            current.setEndTime(endTime);
            current.setLastSyncTime(lastSyncTime);
            current.setLastSyncMatricule(lastSyncMatricule);
            current.setLastSyncDtPointage(lastSyncDtPointage);
            current.setRecordsInserted(recordsInserted);
            current.setRecordsUpdated(recordsUpdated);
            current.setStatus(SynchronizationStatus.SUCCESS);
            return current;
        }

        @Override
        public void markFailed(Long id, LocalDateTime endTime, String errorMessage) {
            current.setEndTime(endTime);
            current.setStatus(current.getLastSyncTime() == null
                    ? SynchronizationStatus.FAILED
                    : SynchronizationStatus.PARTIAL_SUCCESS);
            current.setErrorMessage(errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
