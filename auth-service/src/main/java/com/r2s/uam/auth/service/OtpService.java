package com.r2s.uam.auth.service;

import com.r2s.uam.auth.entity.OtpCode;
import com.r2s.uam.auth.entity.OtpType;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.exception.BadRequestException;
import com.r2s.uam.auth.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public String generateOtp(User user, OtpType type) {
        otpCodeRepository.invalidateUserOtpsByType(user, type);

        String code = generateOtpCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        OtpCode otpCode = OtpCode.builder()
            .user(user)
            .code(code)
            .type(type)
            .expiresAt(expiresAt)
            .used(false)
            .build();

        otpCodeRepository.save(otpCode);
        log.info("Generated OTP for user: {}, type: {}", user.getEmail(), type);

        return code;
    }

    @Transactional
    public boolean validateOtp(User user, String code, OtpType type) {
        Optional<OtpCode> otpCodeOpt = otpCodeRepository
            .findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                user, code, type, LocalDateTime.now()
            );

        if (otpCodeOpt.isEmpty()) {
            log.warn("Invalid or expired OTP for user: {}", user.getEmail());
            return false;
        }

        OtpCode otpCode = otpCodeOpt.get();
        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        log.info("OTP validated successfully for user: {}", user.getEmail());
        return true;
    }

    @Transactional
    public void validateAndVerifyOtp(User user, String code, OtpType type) {
        if (!validateOtp(user, code, type)) {
            throw new BadRequestException("Invalid or expired OTP code");
        }
    }

    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpCodeRepository.deleteExpiredOrUsedOtps(LocalDateTime.now());
        log.info("Cleaned up expired and used OTPs");
    }
}
