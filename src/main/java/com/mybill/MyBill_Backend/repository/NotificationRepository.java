package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Notification n
           SET n.isRead = true,
               n.updatedAt = :updatedAt
           WHERE n.user.id = :userId
             AND n.isRead = false
             AND n.isDeleted = false
           """)
    int markUnreadAsReadForUser(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);
}
