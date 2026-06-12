package com.r2s.uam.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditSearchRequest {

    private UUID userId;
    private String action;
    private String resourceType;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "timestamp";
    private String sortDirection = "DESC";
}
