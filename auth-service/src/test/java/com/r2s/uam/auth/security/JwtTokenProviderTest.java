package com.r2s.uam.auth.security;

import com.r2s.uam.auth.config.JwtProperties;
import com.r2s.uam.auth.entity.Authority;
import com.r2s.uam.auth.entity.Role;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.entity.UserStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@Tag("unit")
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret())
            .thenReturn("test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm!!!");
        when(jwtProperties.getIssuer()).thenReturn("auth-service");
        when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        Authority readUserAuth = Authority.builder().id(1).name("READ_USER").build();
        Authority updateUserAuth = Authority.builder().id(2).name("UPDATE_USER").build();
        Authority deleteUserAuth = Authority.builder().id(3).name("DELETE_USER").build();

        userRole = Role.builder()
            .id(1)
            .name("ROLE_USER")
            .authorities(Set.of(readUserAuth))
            .build();

        adminRole = Role.builder()
            .id(2)
            .name("ROLE_ADMIN")
            .authorities(Set.of(deleteUserAuth, updateUserAuth))
            .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$12$hash")
            .status(UserStatus.ACTIVE)
            .roles(roles)
            .build();
    }

    // ==================== generateAccessToken ====================

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("should generate a valid JWT token")
        void shouldGenerateValidToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWS has 3 parts
        }

        @Test
        @DisplayName("should generate token that passes validation")
        void shouldGenerateTokenThatPassesValidation() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            boolean isValid = jwtTokenProvider.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should include userId claim in token")
        void shouldIncludeUserIdClaim() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            UUID userId = jwtTokenProvider.getUserIdFromToken(token);

            assertThat(userId).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("should include username claim in token")
        void shouldIncludeUsernameClaim() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            String username = jwtTokenProvider.getUsernameFromToken(token);

            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should include roles in token claims")
        void shouldIncludeRolesInClaims() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should include authorities in token claims")
        void shouldIncludeAuthoritiesInClaims() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
            @SuppressWarnings("unchecked")
            List<String> authorities = claims.get("authorities", List.class);

            assertThat(authorities).containsExactlyInAnyOrder("READ_USER", "UPDATE_USER", "DELETE_USER");
        }

        @Test
        @DisplayName("should include subject (userId) in token")
        void shouldIncludeSubjectClaim() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            UUID subject = jwtTokenProvider.getUserIdFromToken(token);

            assertThat(subject.toString()).isEqualTo(testUser.getId().toString());
        }

        @Test
        @DisplayName("should set correct issuer claim")
        void shouldSetIssuerClaim() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

            assertThat(claims.getIssuer()).isEqualTo("auth-service");
        }

        @Test
        @DisplayName("should set expiration in the future")
        void shouldSetExpirationInFuture() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

            assertThat(claims.getExpiration()).isAfter(Instant.now());
            assertThat(claims.getExpiration()).isBefore(Instant.now().plusSeconds(1000));
        }

        @Test
        @DisplayName("should set issued at time")
        void shouldSetIssuedAt() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
        }
    }

    // ==================== generateRefreshToken ====================

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("should generate a unique refresh token each time")
        void shouldGenerateUniqueRefreshToken() {
            String token1 = jwtTokenProvider.generateRefreshToken();
            String token2 = jwtTokenProvider.generateRefreshToken();

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should generate token with UUID prefix and timestamp")
        void shouldGenerateTokenWithUUIDAndTimestamp() {
            String token = jwtTokenProvider.generateRefreshToken();

            assertThat(token).contains("-");
            assertThat(token.split("-")).hasSizeGreaterThan(1);
        }

        @Test
        @DisplayName("should generate non-empty token")
        void shouldGenerateNonEmptyToken() {
            String token = jwtTokenProvider.generateRefreshToken();

            assertThat(token).isNotBlank();
            assertThat(token.length()).isGreaterThan(10);
        }
    }

    // ==================== validateToken ====================

    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            boolean result = jwtTokenProvider.validateToken(token);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            boolean result = jwtTokenProvider.validateToken("malformed.jwt.token");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            boolean result = jwtTokenProvider.validateToken("");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            boolean result = jwtTokenProvider.validateToken(null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for completely fake token")
        void shouldReturnFalseForFakeToken() {
            boolean result = jwtTokenProvider.validateToken(
                "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1dHV0dXQifQ.fake_signature");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for token signed with different key")
        void shouldReturnFalseForTokenSignedWithDifferentKey() {
            // Create a completely separate JwtProperties mock so we don't pollute the main one
            JwtProperties otherProps = mock(JwtProperties.class);
            when(otherProps.getSecret())
                .thenReturn("other-test-secret-must-be-at-least-256-bits-long-for-hs256-algo!!!");
            when(otherProps.getIssuer()).thenReturn("auth-service");
            when(otherProps.getAccessTokenExpiry()).thenReturn(900L);

            JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);
            String tokenFromOther = otherProvider.generateAccessToken(testUser);

            // Validate with the main provider's key (different key) - should fail
            boolean result = jwtTokenProvider.validateToken(tokenFromOther);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            // Create a token with a very short expiry
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(-1L);
            String expiredToken = jwtTokenProvider.generateAccessToken(testUser);

            boolean result = jwtTokenProvider.validateToken(expiredToken);

            assertThat(result).isFalse();
        }
    }

    // ==================== getAllClaimsFromToken ====================

    @Nested
    @DisplayName("getAllClaimsFromToken()")
    class GetAllClaimsFromTokenTests {

        @Test
        @DisplayName("should retrieve all custom claims from token")
        void shouldRetrieveAllClaims() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

            assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
            assertThat(claims.get("username", String.class)).isEqualTo("testuser");
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should throw exception for malformed token when getting all claims")
        void shouldThrowForMalformedTokenOnGetAllClaims() {
            assertThatThrownBy(() -> jwtTokenProvider.getAllClaimsFromToken("not.valid"));
        }

        @Test
        @DisplayName("should retrieve roles list from token claims")
        void shouldRetrieveRolesList() {
            String token = jwtTokenProvider.generateAccessToken(testUser);

            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            assertThat(roles).isNotEmpty();
            assertThat(roles.stream().allMatch(r -> r.startsWith("ROLE_"))).isTrue();
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should generate token for user with no roles")
        void shouldGenerateTokenForUserWithNoRoles() {
            User noRoleUser = User.builder()
                .id(UUID.randomUUID())
                .username("noroleuser")
                .email("norole@example.com")
                .passwordHash("$2a$12$hash")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();

            String token = jwtTokenProvider.generateAccessToken(noRoleUser);

            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("should generate token for user with no authorities")
        void shouldGenerateTokenForUserWithNoAuthorities() {
            Role roleNoAuth = Role.builder()
                .id(1)
                .name("ROLE_BASIC")
                .authorities(new HashSet<>())
                .build();

            User userNoAuth = User.builder()
                .id(UUID.randomUUID())
                .username("noauthuser")
                .email("noauth@example.com")
                .passwordHash("$2a$12$hash")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roleNoAuth))
                .build();

            String token = jwtTokenProvider.generateAccessToken(userNoAuth);

            assertThat(token).isNotNull();
            Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);
            assertThat(claims.get("authorities", List.class)).isEmpty();
        }

        @Test
        @DisplayName("should handle special characters in username")
        void shouldHandleSpecialCharsInUsername() {
            User specialUser = User.builder()
                .id(UUID.randomUUID())
                .username("user_with-dashes_and_underscores")
                .email("special@example.com")
                .passwordHash("$2a$12$hash")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();

            String token = jwtTokenProvider.generateAccessToken(specialUser);

            assertThat(jwtTokenProvider.getUsernameFromToken(token))
                .isEqualTo("user_with-dashes_and_underscores");
        }
    }
}