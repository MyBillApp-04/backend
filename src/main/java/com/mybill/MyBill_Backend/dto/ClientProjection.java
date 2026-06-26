package com.mybill.MyBill_Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientProjection {
    UUID getId();
    String getName();
    String getPhone();
    String getEmail();
    String getAddress();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    String getDeviceId();
    Integer getVersion();
}
