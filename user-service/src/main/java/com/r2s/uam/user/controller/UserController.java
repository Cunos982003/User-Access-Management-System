package com.r2s.uam.user.controller;

import com.r2s.uam.user.dto.request.ChangeEmailRequest;
import com.r2s.uam.user.dto.request.ResetPasswordRequest;
import com.r2s.uam.user.dto.request.UpdateProfileRequest;
import com.r2s.uam.user.dto.request.UpdateStatusRequest;
import com.r2s.uam.user.dto.response.ApiResponse;
import com.r2s.uam.user.dto.response.UserResponse;
import com.r2s.uam.user.security.CustomUserPrincipal;
import com.r2s.uam.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    // User profile endpoints
    @GetMapping("/me")
    @Operation(summary = "Get own profile", description = "Get authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal CustomUserPrincipal principal) {
        UserResponse user = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile", description = "Update authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse user = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me/email")
    @Operation(summary = "Request email change", description = "Request to change email with OTP verification")
    public ResponseEntity<ApiResponse<String>> requestEmailChange(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Valid @RequestBody ChangeEmailRequest request
    ) {
        userService.requestEmailChange(principal.getId(), request);
        String msg = "Verification OTP sent to " + request.getNewEmail() + ". Please verify within 10 minutes.";
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    // Admin endpoints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Search and list all users (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> users = userService.searchUsers(keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Get user details by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user profile (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status", description = "Enable/Disable/Lock user (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        UserResponse user = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin reset password", description = "Admin resets a user's password and sends temp password via email")
    public ResponseEntity<ApiResponse<String>> resetPassword(
        @PathVariable UUID id,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. New password sent to user's email."));
    }
}
