package com.r2s.uam.auth.repository;

import com.r2s.uam.auth.entity.LoginAttempt;
import com.r2s.uam.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByUser(User user);

    List<LoginAttempt> findByUsernameTry(String usernameTry);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE " +
           "la.usernameTry = :username AND la.ipAddress = :ipAddress AND " +
           "la.success = false AND la.attemptedAt > :since")
    long countFailedAttempts(
        @Param("username") String username,
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT la FROM LoginAttempt la WHERE la.attemptedAt < :before")
    List<LoginAttempt> findOldAttempts(@Param("before") LocalDateTime before);
}
