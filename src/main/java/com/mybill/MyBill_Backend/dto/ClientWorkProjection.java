package com.mybill.MyBill_Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientWorkProjection {
    UUID getId();
    UUID getClientId();
    String getClientName();
    String getDescription();
    Double getRate();
    Integer getQuantity();
    Double getAmount();
    Boolean getBilled();
    Boolean getIsDeleted();
    UUID getInvoiceId();
    LocalDateTime getWorkDate();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getDeletedAt();
}
