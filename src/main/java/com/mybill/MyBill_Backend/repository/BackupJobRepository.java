package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BackupJobRepository extends JpaRepository<BackupJob, UUID> {
    List<BackupJob> findByUserIdOrderByCreatedAtDesc(Long userId);
}
