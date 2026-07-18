package com.r2s.uam.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.uam.auth.AuthServiceIntegrationTestBase;
import com.r2s.uam.auth.dto.request.*;
import com.r2s.uam.auth.entity.OtpType;
import com.r2s.uam.auth.entity.RefreshToken;
import com.r2s.uam.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends AuthServiceIntegrationTestBase {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        super.setUpRole();
        super.configureMailSenderMock();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /auth/register - Integration")
    class RegisterIntegrationTests {

        @Test
        @DisplayName("end-to-end: register new user, verify OTP, and login")
        void shouldRegisterAndLogin() throws Exception {
            // Step 1: Register a new user
            RegisterRequest registerRequest = RegisterRequest.builder()
                .username("integrationuser")
                .email("integration@example.com")
                .password("Password123!")
                .fullName("Integration Test")
                .phone("+1234567890")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.username").value("integrationuser"))
                .andExpect(jsonPath("$.data.email").value("integration@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

            // Verify user is stored in DB
            User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo("integration@example.com");
            assertThat(savedUser.getStatus().name()).isEqualTo("PENDING");

            // An OTP code should be generated for the user
            var otpCodes = otpCodeRepository.findByUserAndType(savedUser, OtpType.VERIFY_EMAIL);
            assertThat(otpCodes).isNotEmpty();
            String otpCode = otpCodes.get(0).getCode();
            assertThat(otpCode).hasSize(6);

            // Step 2: Verify OTP with correct code
            VerifyOtpRequest verifyRequest = VerifyOtpRequest.builder()
                .email("integration@example.com")
                .code(otpCode)
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            // Verify user status updated
            savedUser = userRepository.findByUsername("integrationuser").orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getStatus().name()).isEqualTo("ACTIVE");

            // Step 3: Login with verified user
            LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("integrationuser")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("integrationuser"));
        }

        @Test
        @DisplayName("should reject duplicate username registration")
        void shouldRejectDuplicateUsername() throws Exception {
            createActiveUser("existinguser", "existing@test.com", "Password123!");

            RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("different@test.com")
                .password("Password123!")
                .fullName("Different User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
        }

        @Test
        @DisplayName("should reject duplicate email registration")
        void shouldRejectDuplicateEmail() throws Exception {
            createActiveUser("existinguser", "existing@test.com", "Password123!");

            RegisterRequest request = RegisterRequest.builder()
                .username("newusername")
                .email("existing@test.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
        }
    }

    @Nested
    @DisplayName("POST /auth/login - Integration")
    class LoginIntegrationTests {

        @Test
        @DisplayName("should login with username")
        void shouldLoginWithUsername() throws Exception {
            createActiveUser("logintest", "login@example.com", "Password123!");

            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("logintest")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("should login with email")
        void shouldLoginWithEmail() throws Exception {
            createActiveUser("emaillogin", "emaillogin@example.com", "Password123!");

            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("emaillogin@example.com")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should reject login with wrong password")
        void shouldRejectWrongPassword() throws Exception {
            createActiveUser("wrongpassuser", "wrongpass@example.com", "CorrectPassword123!");

            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("wrongpassuser")
                .password("WrongPassword123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject login for non-existent user")
        void shouldRejectNonExistentUser() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("should reject login with unverified (PENDING) user")
        void shouldRejectLoginForPendingUser() throws Exception {
            createPendingUser("pendinguser", "pending@example.com", "Password123!");

            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("pendinguser")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please verify your email first"));
        }
    }

    @Nested
    @DisplayName("POST /auth/verify-otp - Integration")
    class VerifyOtpIntegrationTests {

        @Test
        @DisplayName("should reject wrong OTP code")
        void shouldRejectWrongOtp() throws Exception {
            User user = createPendingUser("otpuser", "otp@example.com", "Password123!");
            createAndSaveOtpCode(user, "999999", OtpType.VERIFY_EMAIL);

            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("otp@example.com")
                .code("123456") // wrong code
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP code"));
        }

        @Test
        @DisplayName("should reject OTP for non-existent user")
        void shouldRejectOtpForNonExistentUser() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("notexist@example.com")
                .code("123456")
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("should reject already used OTP")
        void shouldRejectUsedOtp() throws Exception {
            User user = createPendingUser("usedotpuser", "usedotp@example.com", "Password123!");
            createAndSaveOtpCode(user, "555555", OtpType.VERIFY_EMAIL);
            // First use - should succeed
            VerifyOtpRequest firstUse = VerifyOtpRequest.builder()
                .email("usedotp@example.com")
                .code("555555")
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(firstUse)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /auth/forgot-password - Integration")
    class ForgotPasswordIntegrationTests {

        @Test
        @DisplayName("should send OTP for password reset to existing user")
        void shouldSendOtpForExistingUser() throws Exception {
            User user = createActiveUser("forgotpwduser", "forgot@example.com", "OldPassword123!");

            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("forgot@example.com")
                .build();

            mockMvc.perform(post("/auth/forgot-password")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset OTP sent to your email"));

            // Verify OTP was generated
            var otpCodes = otpCodeRepository.findByUserAndType(user, OtpType.RESET_PASSWORD);
            assertThat(otpCodes).isNotEmpty();
        }

        @Test
        @DisplayName("should return 404 for non-existent email")
        void shouldReturn404ForNonExistentEmail() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("notexist@example.com")
                .build();

            mockMvc.perform(post("/auth/forgot-password")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password - Integration")
    class ResetPasswordIntegrationTests {

        @Test
        @DisplayName("should reset password and revoke all tokens")
        void shouldResetPasswordSuccessfully() throws Exception {
            User user = createActiveUser("resetuser", "reset@example.com", "OldPassword123!");
            String otpCode = createAndSaveOtpCode(user, "888888", OtpType.RESET_PASSWORD).getCode();
            RefreshToken existingToken = createAndSaveRefreshToken(user, false, LocalDateTime.now().plusDays(1));

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("reset@example.com")
                .code(otpCode)
                .newPassword("NewPassword456!")
                .build();

            mockMvc.perform(post("/auth/reset-password")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successful"));

            // Verify old token is revoked
            refreshTokenRepository.findByToken(existingToken.getToken()).ifPresent(token -> {
                assertThat(token.getRevoked()).isTrue();
            });

            // Verify new password works for login
            LoginRequest loginWithNew = LoginRequest.builder()
                .usernameOrEmail("resetuser")
                .password("NewPassword456!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginWithNew)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should reject reset with wrong OTP")
        void shouldRejectResetWithWrongOtp() throws Exception {
            createActiveUser("wrongotpuser", "wrongotp@example.com", "Password123!");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("wrongotp@example.com")
                .code("111222") // wrong
                .newPassword("NewPassword123!")
                .build();

            mockMvc.perform(post("/auth/reset-password")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP code"));
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh-token - Integration")
    class RefreshTokenIntegrationTests {

        @Test
        @DisplayName("should issue new access token when refresh token is valid")
        void shouldIssueNewAccessTokenOnRefresh() throws Exception {
            User user = createActiveUser("refreshuser", "refresh@example.com", "Password123!");
            RefreshToken refreshToken = createAndSaveRefreshToken(user, false, LocalDateTime.now().plusDays(7));

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken.getToken())
                .build();

            MvcResult result = mockMvc.perform(post("/auth/refresh-token")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

            // Old refresh token should be revoked
            refreshTokenRepository.findByToken(refreshToken.getToken()).ifPresent(token -> {
                assertThat(token.getRevoked()).isTrue();
            });
        }

        @Test
        @DisplayName("should reject expired refresh token")
        void shouldRejectExpiredRefreshToken() throws Exception {
            User user = createActiveUser("expireduser", "expired@example.com", "Password123!");
            RefreshToken expiredToken = createAndSaveRefreshToken(
                user, false, LocalDateTime.now().minusDays(1));

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(expiredToken.getToken())
                .build();

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has expired"));
        }

        @Test
        @DisplayName("should reject revoked refresh token")
        void shouldRejectRevokedRefreshToken() throws Exception {
            User user = createActiveUser("revokeduser", "revoked@example.com", "Password123!");
            RefreshToken revokedToken = createAndSaveRefreshToken(
                user, true, LocalDateTime.now().plusDays(7));

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(revokedToken.getToken())
                .build();

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
        }
    }
}