package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}
