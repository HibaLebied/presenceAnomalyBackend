package com.company.attendance.detection.engine;

import com.company.attendance.detection.factory.AnomalyFactory;
import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionResult;
import com.company.attendance.detection.service.AttendanceAnomalyDetectionServiceImpl;
import com.company.attendance.entity.dashboard.AnomalyType;
import com.company.attendance.entity.dashboard.AttendanceRecord;
import com.company.attendance.repository.dashboard.AnomalyRepository;
import com.company.attendance.repository.dashboard.AnomalyTypeRepository;
import com.company.attendance.detection.support.AttendanceRecordTestBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttendanceAnomalyDetectionServiceImplTest {

    @Test
    void shouldDeletePreviousAnomalyAndCreateTheNewOne() {
        TestState state = new TestState();
        AttendanceRecord record = AttendanceRecordTestBuilder.record().mod().build();
        AnomalyType anomalyType = AnomalyType.builder()
                .id(1)
                .code(AnomalyCode.POINTAGE_IMPAIR.name())
                .name("Pointage impair")
                .build();

        AnomalyDetectionEngine engine = context -> {
            state.detections++;
            return DetectionResult.detected(AnomalyCode.POINTAGE_IMPAIR);
        };
        AnomalyRepository anomalyRepository = state.anomalyRepository();
        AnomalyTypeRepository anomalyTypeRepository = state.anomalyTypeRepository(Optional.of(anomalyType));
        AttendanceAnomalyDetectionServiceImpl service = service(engine, anomalyRepository, anomalyTypeRepository);

        service.detectForRecords(List.of(record));

        assertEquals(1, state.deletedRecords);
        assertEquals(1, state.flushes);
        assertEquals(1, state.savedAnomalies);
        assertEquals(AnomalyCode.POINTAGE_IMPAIR.name(), state.requestedAnomalyCode);
    }

    @Test
    void shouldDeletePreviousAnomalyWithoutSavingWhenNoRuleMatches() {
        TestState state = new TestState();
        AttendanceRecord record = AttendanceRecordTestBuilder.record().mod().build();
        AnomalyDetectionEngine engine = context -> {
            state.detections++;
            return DetectionResult.noAnomaly();
        };
        AnomalyRepository anomalyRepository = state.anomalyRepository();
        AnomalyTypeRepository anomalyTypeRepository = state.anomalyTypeRepository(Optional.empty());
        AttendanceAnomalyDetectionServiceImpl service = service(engine, anomalyRepository, anomalyTypeRepository);

        service.detectForRecords(List.of(record));

        assertEquals(1, state.deletedRecords);
        assertEquals(1, state.flushes);
        assertEquals(0, state.savedAnomalies);
        assertEquals(0, state.typeLookups);
    }

    @Test
    void shouldProcessEveryRecordInTheBatch() {
        TestState state = new TestState();
        AttendanceRecord firstRecord = AttendanceRecordTestBuilder.record().mod().build();
        AttendanceRecord secondRecord = AttendanceRecordTestBuilder.record()
                .mod()
                .date(firstRecord.getDtPointage().plusDays(1))
                .build();

        AnomalyDetectionEngine engine = context -> {
            state.detections++;
            return DetectionResult.noAnomaly();
        };
        AnomalyRepository anomalyRepository = state.anomalyRepository();
        AnomalyTypeRepository anomalyTypeRepository = state.anomalyTypeRepository(Optional.empty());
        AttendanceAnomalyDetectionServiceImpl service = service(engine, anomalyRepository, anomalyTypeRepository);

        service.detectForRecords(List.of(firstRecord, secondRecord));

        assertEquals(2, state.deletedRecords);
        assertEquals(2, state.flushes);
        assertEquals(2, state.detections);
    }

    @Test
    void shouldFailWhenDetectedAnomalyTypeDoesNotExist() {
        TestState state = new TestState();
        AttendanceRecord record = AttendanceRecordTestBuilder.record().mod().build();
        AnomalyDetectionEngine engine = context -> {
            state.detections++;
            return DetectionResult.detected(AnomalyCode.ABSENCE_NON_SAISIE);
        };
        AnomalyRepository anomalyRepository = state.anomalyRepository();
        AnomalyTypeRepository anomalyTypeRepository = state.anomalyTypeRepository(Optional.empty());
        AttendanceAnomalyDetectionServiceImpl service = service(engine, anomalyRepository, anomalyTypeRepository);

        assertThrows(IllegalStateException.class, () -> service.detectForRecords(List.of(record)));

        assertEquals(0, state.savedAnomalies);
    }

    private static AttendanceAnomalyDetectionServiceImpl service(
            AnomalyDetectionEngine engine,
            AnomalyRepository anomalyRepository,
            AnomalyTypeRepository anomalyTypeRepository
    ) {
        return new AttendanceAnomalyDetectionServiceImpl(
                engine,
                new AnomalyFactory(),
                anomalyRepository,
                anomalyTypeRepository
        );
    }

    private static final class TestState {
        private int deletedRecords;
        private int flushes;
        private int savedAnomalies;
        private int detections;
        private int typeLookups;
        private String requestedAnomalyCode;

        private AnomalyRepository anomalyRepository() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("deleteByAttendanceRecord")) {
                    deletedRecords++;
                    return null;
                }
                if (method.getName().equals("flush")) {
                    flushes++;
                    return null;
                }
                if (method.getName().equals("save")) {
                    savedAnomalies++;
                    return args[0];
                }
                return defaultValue(method.getReturnType());
            };
            return proxy(AnomalyRepository.class, handler);
        }

        private AnomalyTypeRepository anomalyTypeRepository(Optional<AnomalyType> result) {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("findByCode")) {
                    typeLookups++;
                    requestedAnomalyCode = (String) args[0];
                    return result;
                }
                return defaultValue(method.getReturnType());
            };
            return proxy(AnomalyTypeRepository.class, handler);
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type},
                    handler
            );
        }

        private static Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
