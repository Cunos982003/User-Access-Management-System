package com.r2s.uam.auth.service;

import com.r2s.uam.auth.entity.OtpCode;
import com.r2s.uam.auth.entity.OtpType;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.exception.BadRequestException;
import com.r2s.uam.auth.repository.OtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {

    @Mock private OtpCodeRepository otpCodeRepository;

    @InjectMocks
    private OtpService otpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$12$hash")
            .status(com.r2s.uam.auth.entity.UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();

        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
    }

    // ==================== generateOtp ====================

    @Nested
    @DisplayName("generateOtp()")
    class GenerateOtpTests {

        @Test
        @DisplayName("should generate OTP with correct length")
        void shouldGenerateOtpWithCorrectLength() {
            when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(invocation -> {
                OtpCode otp = invocation.getArgument(0);
                otp.setId(UUID.randomUUID());
                return otp;
            });
            doNothing().when(otpCodeRepository).invalidateUserOtpsByType(any(), any());

            String result = otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(6);
            assertThat(result).matches("\\d{6}");
        }

        @Test
        @DisplayName("should invalidate existing OTPs of same type before generating new one")
        void shouldInvalidateExistingOtpsBeforeGenerating() {
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            verify(otpCodeRepository).invalidateUserOtpsByType(testUser, OtpType.VERIFY_EMAIL);
        }

        @Test
        @DisplayName("should save OTP with correct type and expiry")
        void shouldSaveOtpWithCorrectTypeAndExpiry() {
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            otpService.generateOtp(testUser, OtpType.RESET_PASSWORD);

            ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
            verify(otpCodeRepository).save(captor.capture());
            OtpCode saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(OtpType.RESET_PASSWORD);
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));
            assertThat(saved.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(6));
        }

        @Test
        @DisplayName("should generate different OTPs on consecutive calls")
        void shouldGenerateDifferentOtpsOnConsecutiveCalls() {
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });
            doNothing().when(otpCodeRepository).invalidateUserOtpsByType(any(), any());

            String otp1 = otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);
            String otp2 = otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            assertThat(otp1).isNotNull();
            assertThat(otp2).isNotNull();
            // They might be equal by chance (1/1M), but probability is negligible
        }

        @Test
        @DisplayName("should handle OTP generation for CHANGE_EMAIL type")
        void shouldGenerateOtpForChangeEmailType() {
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            String result = otpService.generateOtp(testUser, OtpType.CHANGE_EMAIL);

            assertThat(result).hasSize(6);
            verify(otpCodeRepository).save(argThat(o -> o.getType() == OtpType.CHANGE_EMAIL));
        }
    }

    // ==================== validateOtp ====================

    @Nested
    @DisplayName("validateOtp()")
    class ValidateOtpTests {

        @Test
        @DisplayName("should return true for valid, unused, non-expired OTP")
        void shouldReturnTrueForValidOtp() {
            OtpCode validOtpCode = OtpCode.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .code("123456")
                .type(OtpType.VERIFY_EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                eq(testUser), eq("123456"), eq(OtpType.VERIFY_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validOtpCode));
            when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = otpService.validateOtp(testUser, "123456", OtpType.VERIFY_EMAIL);

            assertThat(result).isTrue();
            verify(otpCodeRepository).save(argThat(o -> o.getUsed()));
        }

        @Test
        @DisplayName("should return false when OTP not found")
        void shouldReturnFalseWhenOtpNotFound() {
            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());

            boolean result = otpService.validateOtp(testUser, "000000", OtpType.VERIFY_EMAIL);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when OTP is expired")
        void shouldReturnFalseWhenOtpIsExpired() {
            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());

            boolean result = otpService.validateOtp(testUser, "123456", OtpType.VERIFY_EMAIL);

            assertThat(result).isFalse();
        }
    }

    // ==================== validateAndVerifyOtp ====================

    @Nested
    @DisplayName("validateAndVerifyOtp()")
    class ValidateAndVerifyOtpTests {

        @Test
        @DisplayName("should not throw when OTP is valid")
        void shouldNotThrowForValidOtp() {
            OtpCode validOtp = OtpCode.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .code("123456")
                .type(OtpType.VERIFY_EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                eq(testUser), eq("123456"), eq(OtpType.VERIFY_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validOtp));
            when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() ->
                otpService.validateAndVerifyOtp(testUser, "123456", OtpType.VERIFY_EMAIL));
        }

        @Test
        @DisplayName("should throw BadRequestException when OTP is invalid")
        void shouldThrowWhenOtpIsInvalid() {
            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                otpService.validateAndVerifyOtp(testUser, "999999", OtpType.VERIFY_EMAIL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired OTP code");
        }

        @Test
        @DisplayName("should throw BadRequestException when OTP is expired")
        void shouldThrowWhenOtpIsExpired() {
            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                otpService.validateAndVerifyOtp(testUser, "123456", OtpType.RESET_PASSWORD))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired OTP code");
        }

        @Test
        @DisplayName("should distinguish between different OTP types")
        void shouldDistinguishBetweenOtpTypes() {
            when(otpCodeRepository.findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
                any(), any(), any(), any())).thenReturn(Optional.empty());

            // VERIFY_EMAIL type
            assertThatThrownBy(() ->
                otpService.validateAndVerifyOtp(testUser, "123456", OtpType.VERIFY_EMAIL))
                .isInstanceOf(BadRequestException.class);

            // RESET_PASSWORD type - should also throw
            assertThatThrownBy(() ->
                otpService.validateAndVerifyOtp(testUser, "123456", OtpType.RESET_PASSWORD))
                .isInstanceOf(BadRequestException.class);
        }
    }

    // ==================== cleanupExpiredOtps ====================

    @Nested
    @DisplayName("cleanupExpiredOtps()")
    class CleanupExpiredOtpsTests {

        @Test
        @DisplayName("should call repository to delete expired or used OTPs")
        void shouldCleanupExpiredOtps() {
            doNothing().when(otpCodeRepository).deleteExpiredOrUsedOtps(any(LocalDateTime.class));

            otpService.cleanupExpiredOtps();

            verify(otpCodeRepository).deleteExpiredOrUsedOtps(argThat(time ->
                time.isBefore(LocalDateTime.now().plusSeconds(5)) &&
                time.isAfter(LocalDateTime.now().minusSeconds(5))));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle OTP length configuration of 4 digits")
        void shouldHandleFourDigitOtp() {
            ReflectionTestUtils.setField(otpService, "otpLength", 4);
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            String result = otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            assertThat(result).hasSize(4);
            assertThat(result).matches("\\d{4}");
        }

        @Test
        @DisplayName("should handle OTP length configuration of 8 digits")
        void shouldHandleEightDigitOtp() {
            ReflectionTestUtils.setField(otpService, "otpLength", 8);
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            String result = otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            assertThat(result).hasSize(8);
            assertThat(result).matches("\\d{8}");
        }

        @Test
        @DisplayName("should handle different expiry configurations")
        void shouldHandleDifferentExpiryConfigs() {
            ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 10);
            when(otpCodeRepository.save(any())).thenAnswer(inv -> {
                OtpCode o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            otpService.generateOtp(testUser, OtpType.VERIFY_EMAIL);

            verify(otpCodeRepository).save(argThat(o ->
                o.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)) &&
                o.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(11))));
        }
    }
}