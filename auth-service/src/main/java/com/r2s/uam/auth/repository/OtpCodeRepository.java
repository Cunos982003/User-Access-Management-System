package com.r2s.uam.auth.repository;

import com.r2s.uam.auth.entity.OtpCode;
import com.r2s.uam.auth.entity.OtpType;
import com.r2s.uam.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findByUserAndCodeAndTypeAndUsedFalseAndExpiresAtAfter(
        User user, String code, OtpType type, LocalDateTime now
    );

    List<OtpCode> findByUserAndType(User user, OtpType type);

    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.user = :user AND o.type = :type")
    void invalidateUserOtpsByType(@Param("user") User user, @Param("type") OtpType type);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now OR o.used = true")
    void deleteExpiredOrUsedOtps(@Param("now") LocalDateTime now);
}
