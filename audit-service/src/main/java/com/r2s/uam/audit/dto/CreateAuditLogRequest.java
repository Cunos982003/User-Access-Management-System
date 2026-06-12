package com.r2s.uam.audit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {

    private UUID userId;
    private String username;

    @NotBlank(message = "Action is required")
    private String action;

    private String resourceType;
    private UUID resourceId;
    private String description;
    private String ipAddress;
    private String userAgent;

    @NotBlank(message = "Status is required")
    private String status; // SUCCESS, FAILURE

    private String errorMessage;
    private Long durationMs;
    private String requestMethod;
    private String requestPath;
}
