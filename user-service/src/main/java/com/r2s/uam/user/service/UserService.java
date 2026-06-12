package com.r2s.uam.user.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

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

    // Admin operations
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, String status, int page, int size) {
        UserStatus userStatus = status != null ? UserStatus.valueOf(status.toUpperCase()) : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> users = userRepository.searchUsers(keyword, userStatus, pageable);
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

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus().name())
            .roles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLogin(user.getLastLogin())
            .build();
    }
}
