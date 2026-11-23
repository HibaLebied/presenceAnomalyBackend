package com.company.attendance.security.jwt;

import com.company.attendance.security.AuthenticatedUser;
import com.company.attendance.entity.dashboard.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";
    private static final AuthenticatedUser USER = new AuthenticatedUser(
            1L,
            "Admin RH",
            "admin@company.com",
            "password-hash",
            UserRole.ADMIN_RH.name(),
            true
    );

    @Test
    void shouldGenerateTokenWithTheUserEmailAndValidateIt() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken(USER);

        assertEquals(USER.getEmail(), jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, USER));
    }

    @Test
    void shouldRejectAnExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.generateToken(USER);

        assertFalse(jwtService.isTokenValid(token, USER));
    }

    @Test
    void shouldRejectATokenForAnotherUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(USER);
        AuthenticatedUser anotherUser = new AuthenticatedUser(
                2L,
                "Manager RH",
                "manager@company.com",
                "password-hash",
                UserRole.MANAGER.name(),
                true
        );

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    @Test
    void shouldRejectATokenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService("another-secret-with-at-least-32-chars", 60_000);

        String token = issuer.generateToken(USER);

        assertThrows(RuntimeException.class, () -> verifier.extractEmail(token));
    }
}
