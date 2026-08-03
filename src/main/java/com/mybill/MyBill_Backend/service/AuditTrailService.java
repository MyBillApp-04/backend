package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.EntityChangeHistory;
import com.mybill.MyBill_Backend.repository.EntityChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditTrailService {

    private final EntityChangeHistoryRepository entityChangeHistoryRepository;

    @Transactional
    public void logChange(String entityName, UUID entityId, String action, String changeDetails) {
        if (entityName == null || entityId == null || action == null) {
            throw new IllegalArgumentException("Entity name, ID, and action cannot be null");
        }

        EntityChangeHistory history = EntityChangeHistory.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .changeDetails(changeDetails)
                .timestamp(LocalDateTime.now())
                .build();

        entityChangeHistoryRepository.save(history);
        log.debug("Logged change: entity={}, id={}, action={}", entityName, entityId, action);
    }
}
