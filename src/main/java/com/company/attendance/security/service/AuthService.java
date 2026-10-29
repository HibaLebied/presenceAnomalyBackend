package com.company.attendance.security.service;

import com.company.attendance.dto.AuthenticatedUserResponse;
import com.company.attendance.dto.LoginResponse;
import com.company.attendance.entity.dashboard.User;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import com.company.attendance.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationResult login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        User dashboardUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        return issueSession(dashboardUser, user);
    }

    @Transactional("dashboardTransactionManager")
    public AuthenticationResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotatedToken = refreshTokenService.rotate(rawRefreshToken);
        User dashboardUser = userRepository.findByEmail(rotatedToken.userEmail())
                .orElseThrow(() -> new IllegalStateException("Refresh token user no longer exists"));
        AuthenticatedUser user = (AuthenticatedUser) customUserDetailsService
                .loadUserByUsername(dashboardUser.getEmail());

        String accessToken = jwtService.generateToken(user);
        return new AuthenticationResult(
                new LoginResponse(accessToken, toResponse(user)),
                rotatedToken.token().value(),
                rotatedToken.token().expiresAt()
        );
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public AuthenticatedUserResponse currentUser() {
        return toResponse(currentUserService.getCurrentPrincipal());
    }

    private AuthenticatedUserResponse toResponse(AuthenticatedUser user) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private AuthenticationResult issueSession(User dashboardUser, AuthenticatedUser user) {
        String accessToken = jwtService.generateToken(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(dashboardUser);
        return new AuthenticationResult(
                new LoginResponse(accessToken, toResponse(user)),
                refreshToken.value(),
                refreshToken.expiresAt()
        );
    }

    public record AuthenticationResult(
            LoginResponse response,
            String refreshToken,
            java.time.LocalDateTime refreshTokenExpiresAt
    ) {
    }
}
