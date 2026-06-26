package com.mybill.MyBill_Backend.dto.sync;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SyncChangeDto {
    private String changeId;
    private String entityType;
    private String entityId;
    private String operation;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
}
