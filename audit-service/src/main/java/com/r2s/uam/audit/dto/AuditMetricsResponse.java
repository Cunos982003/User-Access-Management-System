package com.r2s.uam.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditMetricsResponse {

    private long totalLogs;
    private long successCount;
    private long failureCount;
    private double failureRate;
    private Map<String, Long> actionStatistics;
    private String period;
}
