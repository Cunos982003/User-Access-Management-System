package com.r2s.uam.auth.service;

import com.r2s.uam.auth.config.JwtProperties;
import com.r2s.uam.auth.entity.RefreshToken;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.exception.UnauthorizedException;
import com.r2s.uam.auth.repository.RefreshTokenRepository;
import com.r2s.uam.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$12$hash")
            .status(com.r2s.uam.auth.entity.UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        testRefreshToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .token("test-refresh-token")
            .deviceId(UUID.randomUUID())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ==================== createRefreshToken ====================

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("should create refresh token with correct expiry time")
        void shouldCreateRefreshTokenWithCorrectExpiry() {
            UUID deviceId = UUID.randomUUID();
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("generated-token");
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800L); // 7 days
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken rt = invocation.getArgument(0);
                rt.setId(UUID.randomUUID());
                return rt;
            });

            RefreshToken result = refreshTokenService.createRefreshToken(testUser, deviceId);

            assertThat(result.getToken()).isEqualTo("generated-token");
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getDeviceId()).isEqualTo(deviceId);
            assertThat(result.getRevoked()).isFalse();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertThat(saved.getExpiresAt())
                .isBefore(LocalDateTime.now().plusDays(8))
                .isAfter(LocalDateTime.now().plusDays(6));
        }

        @Test
        @DisplayName("should create refresh token with null deviceId when not provided")
        void shouldCreateRefreshTokenWithNullDeviceId() {
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("token-with-null-device");
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800L);
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> {
                RefreshToken rt = invocation.getArgument(0);
                rt.setId(UUID.randomUUID());
                return rt;
            });

            RefreshToken result = refreshTokenService.createRefreshToken(testUser, null);

            assertThat(result.getDeviceId()).isNull();
        }

        @Test
        @DisplayName("should use expiry from jwtProperties")
        void shouldUseExpiryFromJwtProperties() {
            UUID deviceId = UUID.randomUUID();
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("token");
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(86400L); // 1 day
            when(refreshTokenRepository.save(any())).thenAnswer(invocation -> {
                RefreshToken rt = invocation.getArgument(0);
                rt.setId(UUID.randomUUID());
                return rt;
            });

            refreshTokenService.createRefreshToken(testUser, deviceId);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            // 1 day = 86400 seconds, allow small tolerance
            assertThat(captor.getValue().getExpiresAt())
                .isBefore(LocalDateTime.now().plusDays(2));
        }
    }

    // ==================== validateRefreshToken ====================

    @Nested
    @DisplayName("validateRefreshToken()")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("should validate and return token when token is valid")
        void shouldValidateValidToken() {
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(testRefreshToken));

            RefreshToken result = refreshTokenService.validateRefreshToken("valid-token");

            assertThat(result).isEqualTo(testRefreshToken);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token not found")
        void shouldThrowWhenTokenNotFound() {
            when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("nonexistent"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token is revoked")
        void shouldThrowWhenTokenIsRevoked() {
            testRefreshToken.setRevoked(true);
            when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(testRefreshToken));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("revoked-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has been revoked");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token is expired")
        void shouldThrowWhenTokenIsExpired() {
            testRefreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(testRefreshToken));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has expired");
        }

        @Test
        @DisplayName("should accept token expiring in the future")
        void shouldAcceptTokenExpiringInFuture() {
            testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByToken("future-token")).thenReturn(Optional.of(testRefreshToken));

            RefreshToken result = refreshTokenService.validateRefreshToken("future-token");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should reject token expiring exactly now")
        void shouldRejectTokenExpiringExactlyNow() {
            testRefreshToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            when(refreshTokenRepository.findByToken("just-expired")).thenReturn(Optional.of(testRefreshToken));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("just-expired"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token has expired");
        }
    }

    // ==================== revokeToken ====================

    @Nested
    @DisplayName("revokeToken()")
    class RevokeTokenTests {

        @Test
        @DisplayName("should call repository to revoke token")
        void shouldRevokeToken() {
            doNothing().when(refreshTokenRepository).revokeToken("token-to-revoke");

            refreshTokenService.revokeToken("token-to-revoke");

            verify(refreshTokenRepository).revokeToken("token-to-revoke");
        }
    }

    // ==================== revokeAllUserTokens ====================

    @Nested
    @DisplayName("revokeAllUserTokens()")
    class RevokeAllUserTokensTests {

        @Test
        @DisplayName("should call repository to revoke all tokens for user")
        void shouldRevokeAllUserTokens() {
            doNothing().when(refreshTokenRepository).revokeAllUserTokens(testUser);

            refreshTokenService.revokeAllUserTokens(testUser);

            verify(refreshTokenRepository).revokeAllUserTokens(testUser);
        }
    }

    // ==================== cleanupExpiredTokens ====================

    @Nested
    @DisplayName("cleanupExpiredTokens()")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("should call repository to delete expired tokens")
        void shouldCleanupExpiredTokens() {
            doNothing().when(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));

            refreshTokenService.cleanupExpiredTokens();

            verify(refreshTokenRepository).deleteExpiredTokens(argThat(time ->
                time.isBefore(LocalDateTime.now().plusSeconds(5)) &&
                time.isAfter(LocalDateTime.now().minusSeconds(5))));
        }
    }
}