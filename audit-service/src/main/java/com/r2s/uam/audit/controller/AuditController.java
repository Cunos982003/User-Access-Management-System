package com.r2s.uam.audit.controller;

import com.r2s.uam.audit.dto.*;
import com.r2s.uam.audit.service.AuditService;
import com.r2s.uam.audit.service.CsvExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit logging and monitoring endpoints")
public class AuditController {

    private final AuditService auditService;
    private final CsvExportService csvExportService;

    @PostMapping("/log")
    @Operation(summary = "Create audit log", description = "Log an audit event (async)")
    public ResponseEntity<ApiResponse<String>> createAuditLog(@Valid @RequestBody CreateAuditLogRequest request) {
        auditService.logEvent(request);
        return ResponseEntity.ok(ApiResponse.success("Audit log queued for processing"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs", description = "Search audit logs with filters")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "timestamp") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        AuditSearchRequest request = new AuditSearchRequest();
        request.setUserId(userId);
        request.setAction(action);
        request.setResourceType(resourceType);
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        Page<AuditLogResponse> logs = auditService.searchAuditLogs(request);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export audit logs to CSV", description = "Export all audit logs as CSV file")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<byte[]> exportToCsv() throws IOException {
        List<AuditLogResponse> logs = auditService.getAllAuditLogs();
        byte[] csvData = csvExportService.exportToCsv(logs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit-logs-" + System.currentTimeMillis() + ".csv");
        headers.setContentLength(csvData.length);

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get audit metrics", description = "Get audit log statistics and metrics")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<AuditMetricsResponse>> getMetrics() {
        AuditMetricsResponse metrics = auditService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
