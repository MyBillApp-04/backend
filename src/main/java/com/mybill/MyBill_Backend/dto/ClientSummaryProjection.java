package com.mybill.MyBill_Backend.dto;

import java.util.UUID;

public interface ClientSummaryProjection {
    UUID getClientId();
    String getClientName();
    Long getTotalWorks();
    Double getTotalAmount();
}