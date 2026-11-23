package com.company.attendance.security.jwt;

import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import com.company.attendance.security.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";
    private final User user = User.builder()
            .id(1L)
            .fullName("Admin RH")
            .email("admin@company.com")
            .passwordHash("password-hash")
            .role(UserRole.ADMIN_RH)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAValidBearerToken() throws ServletException, IOException {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getFullName(), user.getEmail(), user.getPasswordHash(), user.getRole().name(), true
        );
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                new CustomUserDetailsService(repository())
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(principal));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(user.getEmail(), ((AuthenticatedUser) authentication.getPrincipal()).getEmail());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenBearerHeaderIsMissing() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new JwtService(SECRET, 60_000),
                new CustomUserDetailsService(repository())
        );

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldClearAuthenticationWhenBearerTokenIsExpired() throws ServletException, IOException {
        JwtService jwtService = new JwtService(SECRET, -1);
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getFullName(), user.getEmail(), user.getPasswordHash(), user.getRole().name(), true
        );
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                new CustomUserDetailsService(repository())
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(principal));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private UserRepository repository() {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("findByEmail")) {
                return Optional.of(user);
            }
            return null;
        };
        return proxy(UserRepository.class, handler);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
