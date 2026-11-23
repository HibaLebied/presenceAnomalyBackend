package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.RefreshToken;
import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.RefreshTokenRepository;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import com.company.attendance.security.exception.InvalidRefreshTokenException;
import com.company.attendance.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";
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
    void shouldCreateAnAccessTokenAndRefreshTokenAfterSuccessfulLogin() {
        RepositoryDouble repositories = new RepositoryDouble();
        AuthenticatedUser principal = principal();
        AuthService service = service(authenticationManagerReturning(principal), repositories);

        AuthService.AuthenticationResult result = service.login(USER.getEmail(), "password");

        assertEquals(USER.getEmail(), result.response().user().email());
        assertEquals(UserRole.ADMIN_RH.name(), result.response().user().role());
        assertNotEquals("", result.response().token());
        assertEquals(64, repositories.refreshToken.getTokenHash().length());
    }

    @Test
    void shouldPropagateInvalidCredentialsFromTheAuthenticationManager() {
        RepositoryDouble repositories = new RepositoryDouble();
        AuthenticationManager manager = authentication -> {
            throw new BadCredentialsException("bad credentials");
        };
        AuthService service = service(manager, repositories);

        assertThrows(BadCredentialsException.class,
                () -> service.login(USER.getEmail(), "wrong-password"));
    }

    @Test
    void shouldRotateTheRefreshTokenAndIssueANewAccessToken() {
        RepositoryDouble repositories = new RepositoryDouble();
        AuthService service = service(authenticationManagerReturning(principal()), repositories);
        String oldRefreshToken = service.login(USER.getEmail(), "password").refreshToken();

        AuthService.AuthenticationResult refreshed = service.refresh(oldRefreshToken);

        assertNotEquals(oldRefreshToken, refreshed.refreshToken());
        assertEquals(USER.getEmail(), refreshed.response().user().email());
        assertEquals(64, repositories.refreshToken.getTokenHash().length());
    }

    @Test
    void shouldRejectAnInvalidRefreshToken() {
        RepositoryDouble repositories = new RepositoryDouble();
        AuthService service = service(authenticationManagerReturning(principal()), repositories);

        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("invalid-token"));
    }

    @Test
    void shouldRevokeTheRefreshTokenDuringLogout() {
        RepositoryDouble repositories = new RepositoryDouble();
        AuthService service = service(authenticationManagerReturning(principal()), repositories);
        String refreshToken = service.login(USER.getEmail(), "password").refreshToken();

        service.logout(refreshToken);

        assertEquals(true, repositories.refreshToken.getRevokedAt() != null);
    }

    private static AuthService service(AuthenticationManager authenticationManager, RepositoryDouble repositories) {
        RefreshTokenService refreshTokenService = new RefreshTokenService(repositories.refreshTokenRepository());
        try {
            Field field = RefreshTokenService.class.getDeclaredField("expirationTimeMs");
            field.setAccessible(true);
            field.setLong(refreshTokenService, 3_600_000L);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }

        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(repositories.userRepository());
        return new AuthService(
                authenticationManager,
                new JwtService(SECRET, 60_000),
                new CurrentUserService(null),
                userDetailsService,
                repositories.userRepository(),
                refreshTokenService
        );
    }

    private static AuthenticationManager authenticationManagerReturning(AuthenticatedUser principal) {
        return authentication -> new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private static AuthenticatedUser principal() {
        return new AuthenticatedUser(
                USER.getId(),
                USER.getFullName(),
                USER.getEmail(),
                USER.getPasswordHash(),
                USER.getRole().name(),
                true
        );
    }

    private static final class RepositoryDouble {
        private RefreshToken refreshToken;

        private UserRepository userRepository() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("findById") || method.getName().equals("findByEmail")) {
                    return Optional.of(USER);
                }
                return defaultValue(method.getReturnType());
            };
            return proxy(UserRepository.class, handler);
        }

        private RefreshTokenRepository refreshTokenRepository() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("save")) {
                    refreshToken = (RefreshToken) args[0];
                    return refreshToken;
                }
                if (method.getName().equals("findByTokenHash")) {
                    if (refreshToken != null && refreshToken.getTokenHash().equals(args[0])) {
                        return Optional.of(refreshToken);
                    }
                    return Optional.empty();
                }
                return defaultValue(method.getReturnType());
            };
            return proxy(RefreshTokenRepository.class, handler);
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
