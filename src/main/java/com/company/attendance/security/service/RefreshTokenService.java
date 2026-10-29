package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.RefreshToken;
import com.company.attendance.entity.dashboard.User;
import com.company.attendance.repository.dashboard.RefreshTokenRepository;
import com.company.attendance.security.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.refresh-token.expiration-ms:28800000}")
    private long expirationTimeMs;

    @Transactional("dashboardTransactionManager")
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plusNanos(expirationTimeMs * 1_000_000L))
                .createdAt(now)
                .build();

        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, refreshToken.getExpiresAt());
    }

    @Transactional("dashboardTransactionManager")
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken refreshToken = findUsable(rawToken);
        String userEmail = refreshToken.getUser().getEmail();
        String newRawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();

        refreshToken.setTokenHash(hash(newRawToken));
        refreshToken.setExpiresAt(now.plusNanos(expirationTimeMs * 1_000_000L));
        refreshToken.setRevokedAt(null);
        refreshTokenRepository.save(refreshToken);

        return new RotatedRefreshToken(
                userEmail,
                new IssuedRefreshToken(newRawToken, refreshToken.getExpiresAt())
        );
    }

    @Transactional("dashboardTransactionManager")
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private RefreshToken findUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!refreshToken.isUsableAt(LocalDateTime.now())
                || !Boolean.TRUE.equals(refreshToken.getUser().getActive())) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(String value, LocalDateTime expiresAt) {
    }

    public record RotatedRefreshToken(
            String userEmail,
            IssuedRefreshToken token
    ) {
    }
}
