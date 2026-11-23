package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTheAuthenticatedPrincipalFromTheSecurityContext() {
        AuthenticatedUser principal = principal(UserRole.ADMIN_RH.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        CurrentUserService service = new CurrentUserService(null);

        assertEquals(principal, service.getCurrentPrincipal());
        assertTrue(service.hasRole(UserRole.ADMIN_RH));
    }

    @Test
    void shouldRejectRequestsWithoutAnAuthenticatedPrincipal() {
        CurrentUserService service = new CurrentUserService(null);

        assertThrows(AccessDeniedException.class, service::getCurrentPrincipal);
    }

    @Test
    void shouldReturnFalseWhenThePrincipalHasAnotherRole() {
        AuthenticatedUser principal = principal(UserRole.MANAGER.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertFalse(new CurrentUserService(null).hasRole(UserRole.ADMIN_RH));
    }

    private static AuthenticatedUser principal(String role) {
        return new AuthenticatedUser(1L, "User Test", "user@company.com", "hash", role, true);
    }
}
