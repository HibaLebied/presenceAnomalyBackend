package com.company.attendance.security.controller;

import com.company.attendance.entity.dashboard.RefreshToken;
import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.exception.GlobalExceptionHandler;
import com.company.attendance.repository.dashboard.RefreshTokenRepository;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import com.company.attendance.security.exception.InvalidRefreshTokenException;
import com.company.attendance.security.jwt.JwtService;
import com.company.attendance.security.service.AuthService;
import com.company.attendance.security.service.CurrentUserService;
import com.company.attendance.security.service.CustomUserDetailsService;
import com.company.attendance.security.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String EMAIL = "admin@company.com";
    private static final User USER = User.builder()
            .id(1L)
            .fullName("Admin RH")
            .email(EMAIL)
            .passwordHash("hash")
            .role(UserRole.ADMIN_RH)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    private MockMvc mockMvc;
    private AuthService authService;
    private RepositoryDouble repositories;

    @BeforeEach
    void setUp() throws Exception {
        repositories = new RepositoryDouble();
        CustomUserDetailsService userDetailsService =
                new CustomUserDetailsService(repositories.userRepository());
        RefreshTokenService refreshTokenService =
                new RefreshTokenService(repositories.refreshTokenRepository());
        setField(refreshTokenService, "expirationTimeMs", 3_600_000L);

        AuthenticatedUser principal = new AuthenticatedUser(
                USER.getId(), USER.getFullName(), USER.getEmail(), USER.getPasswordHash(),
                USER.getRole().name(), true
        );
        AuthenticationManager authenticationManager = authentication -> {
            if ("wrong-password".equals(authentication.getCredentials())) {
                throw new BadCredentialsException("bad credentials");
            }
            return new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );
        };

        authService = new AuthService(
                authenticationManager,
                new JwtService("test-secret-with-at-least-32-characters", 60_000),
                new CurrentUserService(null),
                userDetailsService,
                repositories.userRepository(),
                refreshTokenService
        );

        AuthController controller = new AuthController(authService);
        setField(controller, "refreshCookiePath", "/api/auth");
        setField(controller, "refreshCookieSameSite", "Lax");
        setField(controller, "refreshCookieSecure", false);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnAccessTokenAndHttpOnlyRefreshCookieAfterValidLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@company.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token="),
                        containsString("Path=/api/auth"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax")
                )));
    }

    @Test
    void shouldReturn401ForInvalidLoginCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@company.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    @Test
    void shouldRotateTheRefreshCookieAndRejectThePreviousToken() throws Exception {
        String oldRefreshToken = authService.login(EMAIL, "correct-password").refreshToken();

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refresh_token", oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token="),
                        not(containsString(oldRefreshToken))
                )));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refresh_token", oldRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRevokeTheRefreshTokenAndClearTheCookieOnLogout() throws Exception {
        String refreshToken = authService.login(EMAIL, "correct-password").refreshToken();

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token="),
                        containsString("Max-Age=0"),
                        containsString("HttpOnly")
                )));

        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refresh(refreshToken)
        );
    }

    @Test
    void shouldRejectInvalidLoginPayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        if (value instanceof Long longValue) {
            field.setLong(target, longValue);
        } else if (value instanceof Boolean booleanValue) {
            field.setBoolean(target, booleanValue);
        } else {
            field.set(target, value);
        }
    }

    private static final class RepositoryDouble {
        private RefreshToken storedRefreshToken;

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
                    storedRefreshToken = (RefreshToken) args[0];
                    return storedRefreshToken;
                }
                if (method.getName().equals("findByTokenHash")) {
                    if (storedRefreshToken != null
                            && storedRefreshToken.getTokenHash().equals(args[0])) {
                        return Optional.of(storedRefreshToken);
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
