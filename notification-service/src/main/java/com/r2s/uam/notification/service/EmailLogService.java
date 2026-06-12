package com.r2s.uam.notification.service;

import com.r2s.uam.notification.dto.EmailLogResponse;
import com.r2s.uam.notification.entity.EmailLog;
import com.r2s.uam.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailLogService {

    private final EmailLogRepository emailLogRepository;

    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return emailLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogsByRecipient(String recipient, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return emailLogRepository.findByRecipient(recipient, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return emailLogRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    private EmailLogResponse toResponse(EmailLog log) {
        return EmailLogResponse.builder()
            .id(log.getId())
            .recipient(log.getRecipient())
            .subject(log.getSubject())
            .templateName(log.getTemplateName())
            .status(log.getStatus())
            .errorMessage(log.getErrorMessage())
            .sentAt(log.getSentAt())
            .createdAt(log.getCreatedAt())
            .retryCount(log.getRetryCount())
            .build();
    }
}
