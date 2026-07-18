package com.r2s.uam.notification.controller;

import com.r2s.uam.notification.dto.ApiResponse;
import com.r2s.uam.notification.dto.BulkEmailRequest;
import com.r2s.uam.notification.dto.EmailLogResponse;
import com.r2s.uam.notification.dto.SendEmailRequest;
import com.r2s.uam.notification.service.EmailLogService;
import com.r2s.uam.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email notification endpoints")
public class NotificationController {

    private final EmailService emailService;
    private final EmailLogService emailLogService;

    @PostMapping("/send")
    @Operation(summary = "Send email", description = "Send email using template (async)")
    public ResponseEntity<ApiResponse<String>> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        emailService.sendEmail(
            request.getRecipient(),
            request.getSubject(),
            request.getTemplateName(),
            request.getVariables()
        );
        return ResponseEntity.ok(ApiResponse.success("Email queued for sending"));
    }

    @PostMapping("/send/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send bulk emails", description = "Admin sends bulk/forced email notifications to multiple recipients")
    public ResponseEntity<ApiResponse<String>> sendBulkEmails(@Valid @RequestBody BulkEmailRequest request) {
        List<EmailService.EmailEntry> entries = request.getEmails().stream()
            .map(item -> new EmailService.EmailEntry(
                item.getRecipient(),
                request.getSubject(),
                request.getTemplateName(),
                item.getVariables()
            ))
            .toList();

        emailService.sendBulkEmails(entries);

        String msg = String.format("Bulk email queued for %d recipients", request.getEmails().size());
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get email logs", description = "Get paginated email logs")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<Page<EmailLogResponse>>> getEmailLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<EmailLogResponse> logs = emailLogService.getEmailLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/recipient/{email}")
    @Operation(summary = "Get logs by recipient", description = "Get email logs for specific recipient")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<Page<EmailLogResponse>>> getLogsByRecipient(
        @PathVariable String email,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<EmailLogResponse> logs = emailLogService.getEmailLogsByRecipient(email, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/status/{status}")
    @Operation(summary = "Get logs by status", description = "Get email logs by status (SENT, FAILED, PENDING)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<Page<EmailLogResponse>>> getLogsByStatus(
        @PathVariable String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<EmailLogResponse> logs = emailLogService.getEmailLogsByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
