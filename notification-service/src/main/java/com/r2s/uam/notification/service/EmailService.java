package com.r2s.uam.notification.service;

import com.r2s.uam.notification.entity.EmailLog;
import com.r2s.uam.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateService templateService;

    @Async
    @Transactional
    public void sendEmail(String recipient, String subject, String templateName, Map<String, String> variables) {
        EmailLog emailLog = EmailLog.builder()
            .recipient(recipient)
            .subject(subject)
            .templateName(templateName)
            .status("PENDING")
            .retryCount(0)
            .build();

        emailLog = emailLogRepository.save(emailLog);

        try {
            String htmlContent = templateService.generateEmailContent(templateName, variables);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@uam.example.com");

            mailSender.send(mimeMessage);

            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);

            log.info("Email sent successfully to: {}, template: {}", recipient, templateName);
        } catch (Exception e) {
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogRepository.save(emailLog);

            log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
        }
    }

    public void sendOtpEmail(String recipient, String otpCode, String purpose) {
        Map<String, String> variables = Map.of(
            "otpCode", otpCode,
            "purpose", purpose
        );
        sendEmail(recipient, "Your OTP Code", "OTP", variables);
    }

    public void sendWelcomeEmail(String recipient, String username) {
        Map<String, String> variables = Map.of("username", username);
        sendEmail(recipient, "Welcome to UAM System", "WELCOME", variables);
    }

    public void sendPasswordResetEmail(String recipient, String otpCode) {
        Map<String, String> variables = Map.of("otpCode", otpCode);
        sendEmail(recipient, "Password Reset Request", "PASSWORD_RESET", variables);
    }
}
