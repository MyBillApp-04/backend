package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.EntityChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EntityChangeHistoryRepository extends JpaRepository<EntityChangeHistory, UUID> {
    List<EntityChangeHistory> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, UUID entityId);
    void deleteByTimestampBefore(LocalDateTime cutoff);
}
