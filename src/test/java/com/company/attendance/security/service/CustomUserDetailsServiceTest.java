package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserDetailsServiceTest {

    @Test
    void shouldMapTheDashboardUserToAnAuthenticatedPrincipal() {
        User user = user(UserRole.MANAGER, true);
        CustomUserDetailsService service = new CustomUserDetailsService(repositoryReturning(user));

        AuthenticatedUser principal = (AuthenticatedUser) service.loadUserByUsername(user.getEmail());

        assertEquals(user.getId(), principal.getId());
        assertEquals(user.getEmail(), principal.getUsername());
        assertEquals(UserRole.MANAGER.name(), principal.getRole());
        assertTrue(principal.isEnabled());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    void shouldKeepAnInactiveUserDisabled() {
        User user = user(UserRole.ADMIN_RH, false);
        CustomUserDetailsService service = new CustomUserDetailsService(repositoryReturning(user));

        AuthenticatedUser principal = (AuthenticatedUser) service.loadUserByUsername(user.getEmail());

        assertFalse(principal.isEnabled());
    }

    @Test
    void shouldRejectAnUnknownEmail() {
        CustomUserDetailsService service = new CustomUserDetailsService(repositoryReturning(null));

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@company.com"));
    }

    private static User user(UserRole role, boolean active) {
        return User.builder()
                .id(1L)
                .fullName("User Test")
                .email("user@company.com")
                .passwordHash("password-hash")
                .role(role)
                .active(active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static UserRepository repositoryReturning(User user) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("findByEmail")) {
                return Optional.ofNullable(user);
            }
            return defaultValue(method.getReturnType());
        };
        return proxy(UserRepository.class, handler);
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
