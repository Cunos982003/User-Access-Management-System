package com.r2s.uam.audit.repository;

import com.r2s.uam.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);

    Page<AuditLog> findByStatus(String status, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.userId = :userId) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:resourceType IS NULL OR al.resourceType = :resourceType) AND " +
           "(:status IS NULL OR al.status = :status) AND " +
           "(:startDate IS NULL OR al.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR al.timestamp <= :endDate)")
    Page<AuditLog> searchAuditLogs(
        @Param("userId") UUID userId,
        @Param("action") String action,
        @Param("resourceType") String resourceType,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action")
    long countByAction(@Param("action") String action);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.status = 'FAILURE'")
    long countFailures();

    @Query("SELECT al.action, COUNT(al) FROM AuditLog al GROUP BY al.action")
    List<Object[]> getActionStatistics();
}
