package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.CustomerNotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerNotificationLogRepository extends JpaRepository<CustomerNotificationLog, UUID> {
    Page<CustomerNotificationLog> findByUserIdOrderByCreatedTimeDesc(Long userId, Pageable pageable);
    
    List<CustomerNotificationLog> findByStatusAndRetryCountLessThan(String status, int maxRetryCount);

    boolean existsByInvoiceIdAndNotificationTypeAndStatus(UUID invoiceId, String notificationType, String status);

    @Query("SELECT MAX(l.createdTime) FROM CustomerNotificationLog l WHERE l.invoice.id = :invoiceId AND l.notificationType = :notificationType AND l.status = 'SENT'")
    java.time.LocalDateTime findLastSentTimeByInvoiceIdAndType(UUID invoiceId, String notificationType);
}
