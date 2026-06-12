package com.r2s.uam.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;
    private Long durationMs;
    private String requestMethod;
    private String requestPath;
}
