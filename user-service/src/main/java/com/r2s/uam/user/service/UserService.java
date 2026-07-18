package com.r2s.uam.user.service;

import com.r2s.uam.user.dto.request.ChangeEmailRequest;
import com.r2s.uam.user.dto.request.ResetPasswordRequest;
import com.r2s.uam.user.dto.request.UpdateProfileRequest;
import com.r2s.uam.user.dto.request.UpdateStatusRequest;
import com.r2s.uam.user.dto.response.UserResponse;
import com.r2s.uam.user.entity.User;
import com.r2s.uam.user.entity.UserStatus;
import com.r2s.uam.user.exception.BadRequestException;
import com.r2s.uam.user.exception.ResourceNotFoundException;
import com.r2s.uam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:8083/api/v1/notifications/send";

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getUsername());

        return toUserResponse(user);
    }

    @Transactional
    public void requestEmailChange(UUID userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        String otpCode = generateTempOtp();
        String cacheKey = "email_change:" + userId;
        redisTemplate.opsForValue().set(cacheKey + ":otp", otpCode, Duration.ofMinutes(10));
        redisTemplate.opsForValue().set(cacheKey + ":newEmail", request.getNewEmail(), Duration.ofMinutes(10));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
            Map.of(
                "recipient", request.getNewEmail(),
                "subject", "Email Change Verification - UAM System",
                "templateName", "EMAIL_CHANGE",
                "variables", Map.of("otpCode", otpCode, "username", user.getUsername())
            ),
            headers
        );

        try {
            restTemplate.exchange(NOTIFICATION_SERVICE_URL, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.warn("Could not send email via notification-service, sending directly: {}", e.getMessage());
            sendEmailDirect(request.getNewEmail(), "Email Change Verification - UAM System",
                "Your OTP code is: " + otpCode + ". This code expires in 10 minutes.");
        }

        log.info("Email change requested for user {}: {}", userId, request.getNewEmail());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, String status, int page, int size) {
        UserStatus userStatus = status != null ? UserStatus.valueOf(status.toUpperCase()) : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));

        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.searchUsers(keyword, userStatus, pageable);
        } else if (userStatus != null) {
            users = userRepository.findByStatus(userStatus, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("User updated by admin: {}", user.getUsername());

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID userId, UpdateStatusRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());

            if (newStatus == UserStatus.PENDING) {
                throw new BadRequestException("Cannot set user status to PENDING");
            }

            user.setStatus(newStatus);
            user = userRepository.save(user);
            log.info("User status updated: {} -> {}", user.getUsername(), newStatus);

            return toUserResponse(user);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }
    }

    @Transactional
    public void resetPassword(UUID userId, ResetPasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
            Map.of(
                "recipient", user.getEmail(),
                "subject", "Password Reset - UAM System",
                "templateName", "PASSWORD_RESET",
                "variables", Map.of("tempPassword", tempPassword, "username", user.getUsername())
            ),
            headers
        );

        try {
            restTemplate.exchange(NOTIFICATION_SERVICE_URL, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.warn("Could not send via notification-service, email body: {}", tempPassword);
        }

        log.info("Admin reset password for user: {}", user.getUsername());
    }

    private String generateTempOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void sendEmailDirect(String to, String subject, String body) {
        log.info("DIRECT EMAIL [{}] to[{}]: {}", subject, to, body);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus().name())
            .roles(user.getRoles() != null
                ? user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet())
                : null)
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLogin(user.getLastLogin())
            .build();
    }
}