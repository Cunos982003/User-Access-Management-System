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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .status(UserStatus.PENDING)
            .roles(roles)
            .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        auditLogService.logSuccess("USER_REGISTER", user.getId(), user.getUsername(),
            "New user registered", null, null);

        String otpCode = otpService.generateOtp(user, OtpType.VERIFY_EMAIL);
        emailService.sendOtpEmail(user.getEmail(), otpCode, "email verification");

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("User is already verified");
        }

        otpService.validateAndVerifyOtp(user, request.getCode(), OtpType.VERIFY_EMAIL);

        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        auditLogService.logSuccess("EMAIL_VERIFIED", user.getId(), user.getUsername(),
            "Email verified and account activated", null, null);

        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        log.info("User verified successfully: {}", user.getUsername());

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(),
                request.getUsernameOrEmail()
            )
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new UnauthorizedException("Please verify your email first");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new UnauthorizedException("Your account has been locked. Please contact support.");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("Your account has been disabled. Please contact support.");
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        UUID deviceId = request.getDeviceId() != null ?
            UUID.fromString(request.getDeviceId()) : UUID.randomUUID();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceId);

        auditLogService.logSuccess("USER_LOGIN", user.getId(), user.getUsername(),
            "User logged in successfully", null, null);
        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken.getToken())
            .tokenType("Bearer")
            .expiresIn(jwtProperties.getAccessTokenExpiry())
            .user(userMapper.toUserResponse(user))
            .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        refreshTokenService.revokeToken(refreshToken.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
            user,
            refreshToken.getDeviceId()
        );

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken.getToken())
            .tokenType("Bearer")
            .expiresIn(jwtProperties.getAccessTokenExpiry())
            .user(userMapper.toUserResponse(user))
            .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }

        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String otpCode = otpService.generateOtp(user, OtpType.RESET_PASSWORD);
        emailService.sendPasswordResetEmail(user.getEmail(), otpCode);

        log.info("Password reset OTP sent to: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.validateAndVerifyOtp(user, request.getCode(), OtpType.RESET_PASSWORD);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }
}
