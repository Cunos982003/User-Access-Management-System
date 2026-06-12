package com.r2s.uam.notification.repository;

import com.r2s.uam.notification.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    Page<EmailLog> findByRecipient(String recipient, Pageable pageable);

    Page<EmailLog> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
