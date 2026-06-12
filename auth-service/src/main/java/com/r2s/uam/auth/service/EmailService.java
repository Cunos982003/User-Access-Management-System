package com.r2s.uam.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your OTP Code - UAM System");
            message.setText(buildOtpEmailBody(otpCode, purpose));

            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to UAM System");
            message.setText(buildWelcomeEmailBody(username));

            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Reset Request - UAM System");
            message.setText(buildPasswordResetEmailBody(otpCode));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

    private String buildOtpEmailBody(String otpCode, String purpose) {
        return String.format("""
            Hello,

            Your OTP code for %s is: %s

            This code will expire in 5 minutes.

            If you did not request this code, please ignore this email.

            Best regards,
            UAM System Team
            """, purpose, otpCode);
    }

    private String buildWelcomeEmailBody(String username) {
        return String.format("""
            Hello %s,

            Welcome to UAM System!

            Your account has been successfully verified and activated.
            You can now log in and start using our services.

            Best regards,
            UAM System Team
            """, username);
    }

    private String buildPasswordResetEmailBody(String otpCode) {
        return String.format("""
            Hello,

            We received a request to reset your password.

            Your password reset OTP code is: %s

            This code will expire in 5 minutes.

            If you did not request a password reset, please ignore this email and your password will remain unchanged.

            Best regards,
            UAM System Team
            """, otpCode);
    }
}
