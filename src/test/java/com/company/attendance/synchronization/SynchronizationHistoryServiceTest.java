package com.company.attendance.synchronization;

import com.company.attendance.entity.dashboard.SynchronizationHistory;
import com.company.attendance.entity.dashboard.SynchronizationStatus;
import com.company.attendance.repository.dashboard.SynchronizationHistoryRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SynchronizationHistoryServiceTest {

    private static final LocalDateTime RECOVERY_TIME = LocalDateTime.of(2026, 11, 22, 14, 0);

    @Test
    void shouldMarkRunningSynchronizationAsFailedWhenNoBatchWasCompleted() {
        SynchronizationHistory running = runningHistory(null);
        HistoryRepositoryDouble repository = new HistoryRepositoryDouble(List.of(running));

        new SynchronizationHistoryService(repository.proxy())
                .recoverRunningSynchronizations(RECOVERY_TIME);

        assertEquals(SynchronizationStatus.FAILED, running.getStatus());
        assertEquals(RECOVERY_TIME, running.getEndTime());
        assertNotNull(running.getErrorMessage());
        assertEquals(1, repository.savedHistories.size());
    }

    @Test
    void shouldMarkRunningSynchronizationAsPartialSuccessWhenProgressWasPersisted() {
        SynchronizationHistory running = runningHistory(LocalDateTime.of(2026, 11, 22, 13, 55));
        running.setLastSyncMatricule("M001");
        running.setLastSyncDtPointage(LocalDateTime.of(2026, 11, 21, 0, 0));
        HistoryRepositoryDouble repository = new HistoryRepositoryDouble(List.of(running));

        new SynchronizationHistoryService(repository.proxy())
                .recoverRunningSynchronizations(RECOVERY_TIME);

        assertEquals(SynchronizationStatus.PARTIAL_SUCCESS, running.getStatus());
        assertEquals(RECOVERY_TIME, running.getEndTime());
        assertEquals(LocalDateTime.of(2026, 11, 22, 13, 55), running.getLastSyncTime());
        assertEquals("M001", running.getLastSyncMatricule());
        assertEquals(1, repository.savedHistories.size());
    }

    @Test
    void shouldMarkBatchFailureAsPartialSuccessWhenPreviousProgressExists() {
        SynchronizationHistory running = runningHistory(LocalDateTime.of(2026, 11, 22, 13, 55));
        HistoryRepositoryDouble repository = new HistoryRepositoryDouble(List.of(running));

        new SynchronizationHistoryService(repository.proxy())
                .markFailed(1L, RECOVERY_TIME, "batch failed");

        assertEquals(SynchronizationStatus.PARTIAL_SUCCESS, running.getStatus());
        assertEquals(RECOVERY_TIME, running.getEndTime());
        assertEquals("batch failed", running.getErrorMessage());
    }

    private static SynchronizationHistory runningHistory(LocalDateTime lastSyncTime) {
        return SynchronizationHistory.builder()
                .id(1L)
                .startTime(RECOVERY_TIME.minusHours(1))
                .lastSyncTime(lastSyncTime)
                .recordsInserted(lastSyncTime == null ? 0 : 500)
                .recordsUpdated(0)
                .status(SynchronizationStatus.RUNNING)
                .build();
    }

    private static final class HistoryRepositoryDouble {
        private final List<SynchronizationHistory> runningHistories;
        private final List<SynchronizationHistory> savedHistories = new ArrayList<>();

        private HistoryRepositoryDouble(List<SynchronizationHistory> runningHistories) {
            this.runningHistories = new ArrayList<>(runningHistories);
        }

        private SynchronizationHistoryRepository proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("findByStatus")) {
                    return runningHistories;
                }
                if (method.getName().equals("findById")) {
                    return Optional.of(runningHistories.get(0));
                }
                if (method.getName().equals("saveAll")) {
                    savedHistories.addAll((List<SynchronizationHistory>) args[0]);
                    return args[0];
                }
                return defaultValue(method.getReturnType());
            };
            return SynchronizationHistoryServiceTest.proxy(SynchronizationHistoryRepository.class, handler);
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
        return null;
    }
}
