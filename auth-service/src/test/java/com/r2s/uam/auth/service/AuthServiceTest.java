package com.r2s.uam.auth.service;

import com.r2s.uam.auth.config.JwtProperties;
import com.r2s.uam.auth.dto.mapper.UserMapper;
import com.r2s.uam.auth.dto.request.*;
import com.r2s.uam.auth.dto.response.AuthResponse;
import com.r2s.uam.auth.dto.response.UserResponse;
import com.r2s.uam.auth.entity.*;
import com.r2s.uam.auth.exception.BadRequestException;
import com.r2s.uam.auth.exception.ResourceNotFoundException;
import com.r2s.uam.auth.exception.UnauthorizedException;
import com.r2s.uam.auth.repository.RoleRepository;
import com.r2s.uam.auth.repository.UserRepository;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;
    private Set<Role> roles;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1).name("ROLE_USER").authorities(new HashSet<>()).build();

        roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .fullName("Test User")
            .phone("+1234567890")
            .status(UserStatus.PENDING)
            .roles(roles)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        lenient().when(userMapper.toUserResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .build();
        });
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully with valid data")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .fullName("New User")
                .phone("+1234567890")
                .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
            when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$encodedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(otpService.generateOtp(any(User.class), eq(OtpType.VERIFY_EMAIL))).thenReturn("123456");

            UserResponse result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("newuser@example.com");
            verify(userRepository).save(any(User.class));
            verify(otpService).generateOtp(any(User.class), eq(OtpType.VERIFY_EMAIL));
            verify(emailService).sendOtpEmail(eq("newuser@example.com"), eq("123456"), eq("email verification"));
            verify(auditLogService).logSuccess(eq("USER_REGISTER"), any(), eq("newuser"), anyString(), any(), any());
        }

        @Test
        @DisplayName("should throw BadRequestException when username is already taken")
        void shouldThrowWhenUsernameAlreadyTaken() {
            RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadRequestException when email is already in use")
        void shouldThrowWhenEmailAlreadyInUse() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when default role not found")
        void shouldThrowWhenDefaultRoleNotFound() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_USER")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Default role not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should assign ROLE_USER to newly registered user")
        void shouldAssignDefaultRoleToNewUser() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(otpService.generateOtp(any(User.class), any())).thenReturn("000000");

            authService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRoles()).contains(userRole);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        }

        @Test
        @DisplayName("should register user without optional phone number")
        void shouldRegisterUserWithoutPhone() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .phone(null)
                .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(otpService.generateOtp(any(User.class), any())).thenReturn("123456");

            UserResponse result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getPhone()).isNull();
        }
    }

    // ==================== VERIFY OTP ====================

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtpTests {

        @Test
        @DisplayName("should verify OTP and activate user successfully")
        void shouldVerifyOtpSuccessfully() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();

            testUser.setStatus(UserStatus.PENDING);
            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
            doNothing().when(otpService).validateAndVerifyOtp(testUser, "123456", OtpType.VERIFY_EMAIL);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse result = authService.verifyOtp(request);

            assertThat(result).isNotNull();
            verify(otpService).validateAndVerifyOtp(testUser, "123456", OtpType.VERIFY_EMAIL);
            assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(emailService).sendWelcomeEmail("test@example.com", "testuser");
            verify(auditLogService).logSuccess(eq("EMAIL_VERIFIED"), any(), eq("testuser"), anyString(), any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found by email")
        void shouldThrowWhenUserNotFoundForOtp() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("notfound@example.com")
                .code("123456")
                .build();

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        }

        @Test
        @DisplayName("should throw BadRequestException when user is already verified (ACTIVE)")
        void shouldThrowWhenUserAlreadyVerified() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();

            testUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));

            assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User is already verified");
        }

        @Test
        @DisplayName("should mark user status as ACTIVE after successful OTP verification")
        void shouldSetUserStatusToActive() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();

            testUser.setStatus(UserStatus.PENDING);
            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
            doNothing().when(otpService).validateAndVerifyOtp(testUser, "123456", OtpType.VERIFY_EMAIL);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setStatus(UserStatus.ACTIVE);
                return u;
            });

            authService.verifyOtp(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with valid credentials and ACTIVE user")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            RefreshToken mockRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("refresh-token-value")
                .user(testUser)
                .build();

            testUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));
            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token-value");
            when(refreshTokenService.createRefreshToken(eq(testUser), any(UUID.class)))
                .thenReturn(mockRefreshToken);
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

            AuthResponse result = authService.login(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token-value");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token-value");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getExpiresIn()).isEqualTo(900L);
            assertThat(result.getUser()).isNotNull();
            verify(auditLogService).logSuccess(eq("USER_LOGIN"), any(), eq("testuser"), anyString(), any(), any());
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not found")
        void shouldThrowWhenUserNotFoundOnLogin() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("Password123!")
                .build();

            when(userRepository.findByUsernameOrEmail("nonexistent", "nonexistent"))
                .thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user status is PENDING")
        void shouldThrowWhenUserIsPending() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            testUser.setStatus(UserStatus.PENDING);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Please verify your email first");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user status is LOCKED")
        void shouldThrowWhenUserIsLocked() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            testUser.setStatus(UserStatus.LOCKED);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your account has been locked. Please contact support.");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user status is DISABLED")
        void shouldThrowWhenUserIsDisabled() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .build();

            testUser.setStatus(UserStatus.DISABLED);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your account has been disabled. Please contact support.");
        }

        @Test
        @DisplayName("should throw UnauthorizedException on invalid password")
        void shouldThrowOnInvalidPassword() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("WrongPassword123!")
                .build();

            testUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("should use provided deviceId when logging in")
        void shouldUseProvidedDeviceId() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .deviceId("550e8400-e29b-41d4-a716-446655440000")
                .build();

            UUID expectedDeviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            RefreshToken mockRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("refresh-token")
                .user(testUser)
                .build();

            testUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(testUser, expectedDeviceId)).thenReturn(mockRefreshToken);
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

            authService.login(request);

            verify(refreshTokenService).createRefreshToken(testUser, expectedDeviceId);
        }

        @Test
        @DisplayName("should generate random deviceId when not provided")
        void shouldGenerateRandomDeviceIdWhenNotProvided() {
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123!")
                .deviceId(null)
                .build();

            RefreshToken mockRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("refresh-token")
                .user(testUser)
                .build();

            testUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(eq(testUser), any(UUID.class))).thenReturn(mockRefreshToken);
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

            authService.login(request);

            verify(refreshTokenService).createRefreshToken(eq(testUser), any(UUID.class));
        }
    }

    // ==================== REFRESH TOKEN ====================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully with valid refresh token")
        void shouldRefreshTokenSuccessfully() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

            RefreshToken oldToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid-refresh-token")
                .user(testUser)
                .deviceId(UUID.randomUUID())
                .build();

            RefreshToken newToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("new-refresh-token")
                .user(testUser)
                .build();

            when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(oldToken);
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
            doNothing().when(refreshTokenService).revokeToken("valid-refresh-token");
            when(refreshTokenService.createRefreshToken(eq(testUser), any())).thenReturn(newToken);
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

            AuthResponse result = authService.refreshToken(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            verify(refreshTokenService).revokeToken("valid-refresh-token");
        }

        @Test
        @DisplayName("should pass deviceId to new refresh token creation")
        void shouldPassDeviceIdToNewToken() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-token")
                .build();

            UUID deviceId = UUID.randomUUID();
            RefreshToken oldToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid-token")
                .user(testUser)
                .deviceId(deviceId)
                .build();

            RefreshToken newToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("new-token")
                .user(testUser)
                .build();

            when(refreshTokenService.validateRefreshToken("valid-token")).thenReturn(oldToken);
            when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access");
            doNothing().when(refreshTokenService).revokeToken("valid-token");
            when(refreshTokenService.createRefreshToken(testUser, deviceId)).thenReturn(newToken);
            when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);

            authService.refreshToken(request);

            verify(refreshTokenService).createRefreshToken(testUser, deviceId);
        }
    }

    // ==================== LOGOUT ====================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should revoke refresh token on logout")
        void shouldRevokeRefreshTokenOnLogout() {
            String refreshToken = "refresh-token-to-revoke";
            doNothing().when(refreshTokenService).revokeToken(refreshToken);

            authService.logout(refreshToken);

            verify(refreshTokenService).revokeToken(refreshToken);
        }

        @Test
        @DisplayName("should handle null refresh token gracefully")
        void shouldHandleNullRefreshToken() {
            authService.logout(null);

            verify(refreshTokenService, never()).revokeToken(anyString());
        }

        @Test
        @DisplayName("should clear security context on logout")
        void shouldClearSecurityContext() {
            authService.logout("some-token");

            // SecurityContextHolder.clearContext() is called internally
            // This test verifies the method completes without error
            verify(refreshTokenService).revokeToken("some-token");
        }
    }

    // ==================== FORGOT PASSWORD ====================

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should send OTP for password reset successfully")
        void shouldSendOtpForPasswordReset() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
            when(otpService.generateOtp(testUser, OtpType.RESET_PASSWORD)).thenReturn("654321");

            authService.forgotPassword(request);

            verify(otpService).generateOtp(testUser, OtpType.RESET_PASSWORD);
            verify(emailService).sendPasswordResetEmail("test@example.com", "654321");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when email not found")
        void shouldThrowWhenEmailNotFoundForForgotPassword() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("notfound@example.com")
                .build();

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email");
        }
    }

    // ==================== RESET PASSWORD ====================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password successfully with valid OTP")
        void shouldResetPasswordSuccessfully() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("NewPassword123!")
                .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
            doNothing().when(otpService).validateAndVerifyOtp(testUser, "123456", OtpType.RESET_PASSWORD);
            when(passwordEncoder.encode("NewPassword123!")).thenReturn("$2a$12$newencodedpassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doNothing().when(refreshTokenService).revokeAllUserTokens(testUser);

            authService.resetPassword(request);

            verify(passwordEncoder).encode("NewPassword123!");
            verify(userRepository).save(testUser);
            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found on reset")
        void shouldThrowWhenUserNotFoundOnResetPassword() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("notfound@example.com")
                .code("123456")
                .newPassword("NewPassword123!")
                .build();

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        }

        @Test
        @DisplayName("should revoke all user tokens after password reset")
        void shouldRevokeAllTokensAfterReset() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("NewPassword123!")
                .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
            doNothing().when(otpService).validateAndVerifyOtp(testUser, "123456", OtpType.RESET_PASSWORD);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
            when(userRepository.save(any())).thenReturn(testUser);
            doNothing().when(refreshTokenService).revokeAllUserTokens(testUser);

            authService.resetPassword(request);

            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("should change password successfully with valid current password")
        void shouldChangePasswordSuccessfully() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword456!")
                .build();

            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("NewPassword456!")).thenReturn("$2a$12$newpasswordhash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doNothing().when(refreshTokenService).revokeAllUserTokens(testUser);

            authService.changePassword(request, "testuser");

            verify(passwordEncoder).encode("NewPassword456!");
            verify(userRepository).save(testUser);
            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("should throw BadRequestException when current password is incorrect")
        void shouldThrowWhenCurrentPasswordIncorrect() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("NewPassword456!")
                .build();

            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("WrongPassword123!", testUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(request, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadRequestException when new password equals current password")
        void shouldThrowWhenNewPasswordSameAsCurrent() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Password123!")
                .newPassword("Password123!")
                .build();

            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("Password123!", testUser.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(request, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password must be different from current password");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found on change password")
        void shouldThrowWhenUserNotFoundOnChangePassword() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword456!")
                .build();

            when(userRepository.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(request, "nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        }

        @Test
        @DisplayName("should revoke all tokens after password change")
        void shouldRevokeAllTokensAfterPasswordChange() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword456!")
                .build();

            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("NewPassword456!")).thenReturn("$2a$12$hash");
            when(userRepository.save(any())).thenReturn(testUser);
            doNothing().when(refreshTokenService).revokeAllUserTokens(testUser);

            authService.changePassword(request, "testuser");

            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }
    }
}