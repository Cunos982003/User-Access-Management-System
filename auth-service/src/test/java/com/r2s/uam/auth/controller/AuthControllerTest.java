package com.r2s.uam.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.uam.auth.dto.request.*;
import com.r2s.uam.auth.dto.response.ApiResponse;
import com.r2s.uam.auth.dto.response.AuthResponse;
import com.r2s.uam.auth.dto.response.UserResponse;
import com.r2s.uam.auth.exception.BadRequestException;
import com.r2s.uam.auth.exception.GlobalExceptionHandler;
import com.r2s.uam.auth.exception.ResourceNotFoundException;
import com.r2s.uam.auth.exception.UnauthorizedException;
import com.r2s.uam.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("web")
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserResponse testUserResponse;
    private AuthResponse testAuthResponse;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        HandlerMethodArgumentResolver userDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(userDetailsResolver)
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserResponse = UserResponse.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .fullName("Test User")
            .status("PENDING")
            .roles(Set.of("ROLE_USER"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        testAuthResponse = AuthResponse.builder()
            .accessToken("access-token-value")
            .refreshToken("refresh-token-value")
            .tokenType("Bearer")
            .expiresIn(900L)
            .user(testUserResponse)
            .build();

        mockUserDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("should return 201 when registration is successful")
        void shouldReturn201OnSuccessfulRegister() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void shouldReturn400WhenUsernameBlank() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("not-an-email")
                .password("Password123!")
                .fullName("New User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when password does not meet policy")
        void shouldReturn400WhenPasswordDoesNotMeetPolicy() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("weak") // Missing: uppercase, digit, special char, min length
                .fullName("New User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when fullName is blank")
        void shouldReturn400WhenFullNameBlank() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 409 when username already taken")
        void shouldReturn409WhenUsernameTaken() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Username is already taken"));

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
        }

        @Test
        @DisplayName("should accept register request with valid phone number")
        void shouldAcceptValidPhoneNumber() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .phone("+84 123 456 789")
                .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 when username contains invalid characters")
        void shouldReturn400WhenUsernameInvalidChars() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("user@name!")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== VERIFY OTP ====================

    @Nested
    @DisplayName("POST /auth/verify-otp")
    class VerifyOtpEndpointTests {

        @Test
        @DisplayName("should return 200 when OTP is valid")
        void shouldReturn200OnValidOtp() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();

            when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
        }

        @Test
        @DisplayName("should return 400 when OTP code is not 6 digits")
        void shouldReturn400WhenOtpNotSixDigits() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("12345") // only 5 digits
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("notfound@example.com")
                .code("123456")
                .build();

            when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("should return 400 when OTP is invalid")
        void shouldReturn400WhenOtpInvalid() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("000000")
                .build();

            when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new BadRequestException("Invalid or expired OTP code"));

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP code"));
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("")
                .code("123456")
                .build();

            mockMvc.perform(post("/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("should return 200 and tokens on successful login")
        void shouldReturn200OnSuccessfulLogin() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(testAuthResponse);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-value"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401OnInvalidCredentials() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("WrongPassword123!")
                .build();

            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("should return 401 when user status is PENDING")
        void shouldReturn401WhenUserIsPending() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Please verify your email first"));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please verify your email first"));
        }

        @Test
        @DisplayName("should return 401 when user is locked")
        void shouldReturn401WhenUserIsLocked() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Your account has been locked. Please contact support."));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account has been locked. Please contact support."));
        }

        @Test
        @DisplayName("should return 401 when user is disabled")
        void shouldReturn401WhenUserIsDisabled() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Your account has been disabled. Please contact support."));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account has been disabled. Please contact support."));
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void shouldReturn400WhenUsernameBlank() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("")
                .password("Password123!")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void shouldReturn400WhenPasswordBlank() throws Exception {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("")
                .build();

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== REFRESH TOKEN ====================

    @Nested
    @DisplayName("POST /auth/refresh-token")
    class RefreshTokenEndpointTests {

        @Test
        @DisplayName("should return 200 when refresh token is valid")
        void shouldReturn200OnValidRefreshToken() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(testAuthResponse);

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        @DisplayName("should return 401 when refresh token is invalid")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
        }

        @Test
        @DisplayName("should return 400 when refresh token is blank")
        void shouldReturn400WhenRefreshTokenBlank() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGOUT ====================

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutEndpointTests {

        @Test
        @DisplayName("should return 200 on successful logout")
        void shouldReturn200OnLogout() throws Exception {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("token-to-revoke")
                .build();

            doNothing().when(authService).logout(anyString());

            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

            verify(authService).logout("token-to-revoke");
        }

        @Test
        @DisplayName("should handle null request body gracefully")
        void shouldHandleNullRequestBody() throws Exception {
            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(null)))
                .andExpect(status().isOk());
        }
    }

    // ==================== FORGOT PASSWORD ====================

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPasswordEndpointTests {

        @Test
        @DisplayName("should return 200 when OTP is sent successfully")
        void shouldReturn200OnForgotPassword() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

            doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

            mockMvc.perform(post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset OTP sent to your email"));
        }

        @Test
        @DisplayName("should return 404 when email not found")
        void shouldReturn404WhenEmailNotFound() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("notfound@example.com")
                .build();

            doThrow(new ResourceNotFoundException("User not found with email: notfound@example.com"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

            mockMvc.perform(post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("invalid-email")
                .build();

            mockMvc.perform(post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== RESET PASSWORD ====================

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPasswordEndpointTests {

        @Test
        @DisplayName("should return 200 when password is reset successfully")
        void shouldReturn200OnPasswordReset() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("NewPassword123!")
                .build();

            doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successful"));
        }

        @Test
        @DisplayName("should return 400 when new password does not meet policy")
        void shouldReturn400WhenNewPasswordWeak() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("weak")
                .build();

            mockMvc.perform(post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when OTP code is not 6 digits")
        void shouldReturn400WhenOtpCodeNotSixDigits() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("12345")
                .newPassword("NewPassword123!")
                .build();

            mockMvc.perform(post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when OTP is invalid")
        void shouldReturn400WhenOtpInvalid() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("000000")
                .newPassword("NewPassword123!")
                .build();

            doThrow(new BadRequestException("Invalid or expired OTP code"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP code"));
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @Nested
    @DisplayName("POST /auth/change-password")
    class ChangePasswordEndpointTests {

        @Test
        @DisplayName("should return 200 when password is changed successfully")
        void shouldReturn200OnPasswordChange() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword456!")
                .build();

            doNothing().when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            mockMvc.perform(post("/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .principal(() -> "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("should return 400 when current password is incorrect")
        void shouldReturn400WhenCurrentPasswordIncorrect() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("NewPassword456!")
                .build();

            doThrow(new BadRequestException("Current password is incorrect"))
                .when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            mockMvc.perform(post("/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .principal(() -> "testuser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("should return 400 when new password is same as current")
        void shouldReturn400WhenNewPasswordSameAsCurrent() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Password123!")
                .newPassword("Password123!")
                .build();

            doThrow(new BadRequestException("New password must be different from current password"))
                .when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            mockMvc.perform(post("/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .principal(() -> "testuser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be different from current password"));
        }

        @Test
        @DisplayName("should return 400 when new password does not meet policy")
        void shouldReturn400WhenNewPasswordDoesNotMeetPolicy() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("weak")
                .build();

            mockMvc.perform(post("/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .principal(() -> "testuser"))
                .andExpect(status().isBadRequest());
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should return 400 when request body is missing entirely")
        void shouldReturn400WhenNoRequestBody() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return proper error structure for validation failures")
        void shouldReturnProperErrorStructure() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .username("") // empty
                .email("bad") // invalid
                .password("x") // too short
                .fullName("") // empty
                .build();

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(400));
        }
    }
}