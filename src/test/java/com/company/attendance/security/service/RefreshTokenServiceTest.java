package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.RefreshToken;
import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.RefreshTokenRepository;
import com.company.attendance.security.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceTest {

    private static final User USER = User.builder()
            .id(1L)
            .fullName("Admin RH")
            .email("admin@company.com")
            .passwordHash("password-hash")
            .role(UserRole.ADMIN_RH)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    @Test
    void shouldStoreOnlyTheHashWhenIssuingARefreshToken() {
        RepositoryDouble repository = new RepositoryDouble();
        RefreshTokenService service = service(repository, 3_600_000L);

        RefreshTokenService.IssuedRefreshToken issued = service.issue(USER);

        assertNotNull(issued.value());
        assertNotNull(repository.stored);
        assertNotEquals(issued.value(), repository.stored.getTokenHash());
        assertEquals(64, repository.stored.getTokenHash().length());
        assertEquals(hash(issued.value()), repository.stored.getTokenHash());
        assertTrue(repository.stored.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldRotateTheSameDatabaseRowAndInvalidateTheOldRawToken() {
        RepositoryDouble repository = new RepositoryDouble();
        RefreshTokenService service = service(repository, 3_600_000L);
        String oldRawToken = service.issue(USER).value();
        RefreshToken storedRow = repository.stored;

        RefreshTokenService.RotatedRefreshToken rotated = service.rotate(oldRawToken);

        assertEquals(storedRow, repository.stored);
        assertNotEquals(oldRawToken, rotated.token().value());
        assertEquals(hash(rotated.token().value()), storedRow.getTokenHash());
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(oldRawToken));
    }

    @Test
    void shouldRejectARevokedRefreshToken() {
        RepositoryDouble repository = new RepositoryDouble();
        RefreshTokenService service = service(repository, 3_600_000L);
        String rawToken = service.issue(USER).value();

        service.revoke(rawToken);

        assertNotNull(repository.stored.getRevokedAt());
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(rawToken));
    }

    @Test
    void shouldRejectAnExpiredRefreshToken() {
        RepositoryDouble repository = new RepositoryDouble();
        RefreshTokenService service = service(repository, -1L);
        String rawToken = service.issue(USER).value();

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(rawToken));
    }

    @Test
    void shouldIgnoreLogoutWhenNoRefreshTokenWasProvided() {
        RepositoryDouble repository = new RepositoryDouble();
        RefreshTokenService service = service(repository, 3_600_000L);

        service.revoke(null);
        service.revoke("   ");

        assertEquals(0, repository.saveCalls);
    }

    private static RefreshTokenService service(RepositoryDouble repository, long expirationMs) {
        RefreshTokenService service = new RefreshTokenService(repository.proxy());
        try {
            Field field = RefreshTokenService.class.getDeclaredField("expirationTimeMs");
            field.setAccessible(true);
            field.setLong(service, expirationMs);
            return service;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class RepositoryDouble {
        private RefreshToken stored;
        private int saveCalls;

        private RefreshTokenRepository proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("save")) {
                    stored = (RefreshToken) args[0];
                    saveCalls++;
                    return stored;
                }
                if (method.getName().equals("findByTokenHash")) {
                    if (stored != null && stored.getTokenHash().equals(args[0])) {
                        return Optional.of(stored);
                    }
                    return Optional.empty();
                }
                return defaultValue(method.getReturnType());
            };
            return RefreshTokenServiceTest.proxy(RefreshTokenRepository.class, handler);
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
