package com.r2s.uam.audit.service;

import com.r2s.uam.audit.dto.AuditLogResponse;
import com.r2s.uam.audit.dto.AuditMetricsResponse;
import com.r2s.uam.audit.dto.AuditSearchRequest;
import com.r2s.uam.audit.dto.CreateAuditLogRequest;
import com.r2s.uam.audit.entity.AuditLog;
import com.r2s.uam.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void logEvent(CreateAuditLogRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .action(request.getAction())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .description(request.getDescription())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .status(request.getStatus())
                .errorMessage(request.getErrorMessage())
                .durationMs(request.getDurationMs())
                .requestMethod(request.getRequestMethod())
                .requestPath(request.getRequestPath())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: action={}, user={}, status={}",
                request.getAction(), request.getUsername(), request.getStatus());
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(AuditSearchRequest request) {
        Sort sort = Sort.by(
            request.getSortDirection().equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortBy()
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<AuditLog> logs = auditLogRepository.searchAuditLogs(
            request.getUserId(),
            request.getAction(),
            request.getResourceType(),
            request.getStatus(),
            request.getStartDate(),
            request.getEndDate(),
            pageable
        );

        return logs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AuditMetricsResponse getMetrics() {
        long totalLogs = auditLogRepository.count();
        long failureCount = auditLogRepository.countFailures();
        long successCount = totalLogs - failureCount;
        double failureRate = totalLogs > 0 ? (double) failureCount / totalLogs * 100 : 0;

        List<Object[]> actionStats = auditLogRepository.getActionStatistics();
        Map<String, Long> actionStatistics = new HashMap<>();
        for (Object[] stat : actionStats) {
            actionStatistics.put((String) stat[0], (Long) stat[1]);
        }

        return AuditMetricsResponse.builder()
            .totalLogs(totalLogs)
            .successCount(successCount)
            .failureCount(failureCount)
            .failureRate(Math.round(failureRate * 100.0) / 100.0)
            .actionStatistics(actionStatistics)
            .period("all-time")
            .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
            .id(log.getId())
            .userId(log.getUserId())
            .username(log.getUsername())
            .action(log.getAction())
            .resourceType(log.getResourceType())
            .resourceId(log.getResourceId())
            .description(log.getDescription())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .status(log.getStatus())
            .errorMessage(log.getErrorMessage())
            .timestamp(log.getTimestamp())
            .durationMs(log.getDurationMs())
            .requestMethod(log.getRequestMethod())
            .requestPath(log.getRequestPath())
            .build();
    }
}
