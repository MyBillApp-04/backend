package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.EmailLog;
import com.mybill.MyBill_Backend.entity.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    List<EmailLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<EmailLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<EmailLog> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, EmailStatus status, Pageable pageable);
    Optional<EmailLog> findByEmailLogIdAndUserId(UUID emailLogId, Long userId);
    boolean existsByInvoiceIdAndTemplateTypeAndCreatedAtAfter(UUID invoiceId, String templateType, LocalDateTime since);
    List<EmailLog> findTop25ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(EmailStatus status, LocalDateTime now);
}
