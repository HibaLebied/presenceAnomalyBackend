package com.company.attendance.security;

import com.company.attendance.entity.dashboard.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

    @Test
    void shouldExposeTheRoleWithTheSpringSecurityPrefix() {
        AuthenticatedUser user = new AuthenticatedUser(
                1L, "Admin RH", "admin@company.com", "hash", UserRole.ADMIN_RH.name(), true
        );

        assertEquals("ROLE_ADMIN_RH", user.getAuthorities().iterator().next().getAuthority());
        assertTrue(user.isEnabled());
    }

    @Test
    void shouldDisableAnInactivePrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(
                1L, "Manager RH", "manager@company.com", "hash", UserRole.MANAGER.name(), false
        );

        assertFalse(user.isEnabled());
    }
}
