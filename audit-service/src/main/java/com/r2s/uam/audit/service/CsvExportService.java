package com.r2s.uam.audit.service;

import com.r2s.uam.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvExportService {

    private static final String[] HEADERS = {
        "ID", "User ID", "Username", "Action", "Resource Type", "Resource ID",
        "Description", "IP Address", "Status", "Error Message", "Timestamp",
        "Duration (ms)", "Request Method", "Request Path"
    };

    public byte[] exportToCsv(List<AuditLogResponse> auditLogs) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(HEADERS))) {

            for (AuditLogResponse log : auditLogs) {
                csvPrinter.printRecord(
                    log.getId(),
                    log.getUserId(),
                    log.getUsername(),
                    log.getAction(),
                    log.getResourceType(),
                    log.getResourceId(),
                    log.getDescription(),
                    log.getIpAddress(),
                    log.getStatus(),
                    log.getErrorMessage(),
                    log.getTimestamp(),
                    log.getDurationMs(),
                    log.getRequestMethod(),
                    log.getRequestPath()
                );
            }

            csvPrinter.flush();
            log.info("Exported {} audit logs to CSV", auditLogs.size());
            return outputStream.toByteArray();
        }
    }
}
