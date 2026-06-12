package com.r2s.uam.auth.service;

import com.r2s.uam.auth.config.JwtProperties;
import com.r2s.uam.auth.entity.RefreshToken;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.exception.UnauthorizedException;
import com.r2s.uam.auth.repository.RefreshTokenRepository;
import com.r2s.uam.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken createRefreshToken(User user, UUID deviceId) {
        String token = jwtTokenProvider.generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusSeconds(jwtProperties.getRefreshTokenExpiry());

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .deviceId(deviceId)
            .expiresAt(expiresAt)
            .revoked(false)
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeToken(token);
        log.info("Refresh token revoked");
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("All refresh tokens revoked for user: {}", user.getUsername());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }
}
