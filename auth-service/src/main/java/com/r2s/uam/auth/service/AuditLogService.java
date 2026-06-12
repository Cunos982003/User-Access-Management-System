package com.r2s.uam.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuditLogService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${audit.service.url:http://localhost:8082/api/v1}")
    private String auditServiceUrl;

    @Async
    public void logSuccess(String action, UUID userId, String username, String description, String ipAddress, String userAgent) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("username", username);
            request.put("action", action);
            request.put("resourceType", "USER");
            request.put("description", description);
            request.put("ipAddress", ipAddress);
            request.put("userAgent", userAgent);
            request.put("status", "SUCCESS");

            restTemplate.postForEntity(auditServiceUrl + "/audit/log", request, String.class);
            log.debug("Audit log sent: action={}, user={}", action, username);
        } catch (Exception e) {
            log.error("Failed to send audit log: {}", e.getMessage());
        }
    }

    @Async
    public void logFailure(String action, String username, String description, String errorMessage, String ipAddress, String userAgent) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("username", username);
            request.put("action", action);
            request.put("resourceType", "USER");
            request.put("description", description);
            request.put("ipAddress", ipAddress);
            request.put("userAgent", userAgent);
            request.put("status", "FAILURE");
            request.put("errorMessage", errorMessage);

            restTemplate.postForEntity(auditServiceUrl + "/audit/log", request, String.class);
            log.debug("Audit failure log sent: action={}, user={}", action, username);
        } catch (Exception e) {
            log.error("Failed to send audit failure log: {}", e.getMessage());
        }
    }
}
