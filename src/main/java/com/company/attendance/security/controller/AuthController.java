package com.company.attendance.security.controller;

import com.company.attendance.dto.AuthenticatedUserResponse;
import com.company.attendance.dto.LoginRequest;
import com.company.attendance.dto.LoginResponse;
import com.company.attendance.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;

    @Value("${security.refresh-token.cookie-path:/api/auth}")
    private String refreshCookiePath;

    @Value("${security.refresh-token.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${security.refresh-token.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthenticationResult result = authService.login(request.email(), request.password());
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        AuthService.AuthenticationResult result = authService.refresh(refreshToken);
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me() {
        return ResponseEntity.ok(authService.currentUser());
    }

    private ResponseEntity<LoginResponse> withRefreshCookie(AuthService.AuthenticationResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(result.response());
    }

    private ResponseCookie refreshCookie(AuthService.AuthenticationResult result) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(java.time.Duration.between(
                        java.time.LocalDateTime.now(),
                        result.refreshTokenExpiresAt()
                ))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(java.time.Duration.ZERO)
                .build();
    }
}
