package com.r2s.uam.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailRequest {

    @NotEmpty(message = "At least one recipient is required")
    private List<BulkEmailItem> emails;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkEmailItem {
        @NotBlank(message = "Recipient is required")
        private String recipient;

        private Map<String, String> variables;
    }
}