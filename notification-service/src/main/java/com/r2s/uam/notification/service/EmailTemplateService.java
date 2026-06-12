package com.r2s.uam.notification.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailTemplateService {

    public String generateEmailContent(String templateName, Map<String, String> variables) {
        return switch (templateName) {
            case "OTP" -> generateOtpTemplate(variables);
            case "WELCOME" -> generateWelcomeTemplate(variables);
            case "PASSWORD_RESET" -> generatePasswordResetTemplate(variables);
            default -> generateDefaultTemplate(variables);
        };
    }

    private String generateOtpTemplate(Map<String, String> variables) {
        String otpCode = variables.getOrDefault("otpCode", "000000");
        String purpose = variables.getOrDefault("purpose", "verification");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .otp-box { background: #f4f4f4; padding: 20px; text-align: center; border-radius: 5px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your OTP Code</h2>
                    <p>You requested an OTP code for <strong>%s</strong>.</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p>This code will expire in 5 minutes.</p>
                    <p>If you did not request this code, please ignore this email.</p>
                    <div class="footer">
                        <p>This is an automated message from UAM System.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(purpose, otpCode);
    }

    private String generateWelcomeTemplate(Map<String, String> variables) {
        String username = variables.getOrDefault("username", "User");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #007bff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Welcome to UAM System!</h2>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your account has been successfully activated. You can now access all features of our platform.</p>
                        <p>Thank you for joining us!</p>
                    </div>
                    <div class="footer">
                        <p>UAM System - User Access Management</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username);
    }

    private String generatePasswordResetTemplate(Map<String, String> variables) {
        String otpCode = variables.getOrDefault("otpCode", "000000");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .otp-box { background: #f4f4f4; padding: 20px; text-align: center; border-radius: 5px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #dc3545; letter-spacing: 5px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Reset Request</h2>
                    <div class="warning">
                        <strong>Security Alert:</strong> A password reset was requested for your account.
                    </div>
                    <p>Use the following OTP code to reset your password:</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p>This code will expire in 5 minutes.</p>
                    <p><strong>If you did not request this, please ignore this email and contact support immediately.</strong></p>
                    <div class="footer">
                        <p>UAM System - User Access Management</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otpCode);
    }

    private String generateDefaultTemplate(Map<String, String> variables) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Notification from UAM System</h2>
                    <p>This is an automated notification.</p>
                </div>
            </body>
            </html>
            """;
    }
}
