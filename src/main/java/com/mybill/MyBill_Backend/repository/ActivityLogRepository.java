package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}
