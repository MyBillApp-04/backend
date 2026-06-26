package com.mybill.MyBill_Backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {
    private UUID clientId;
    private List<UUID> workIds;

    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;

    private Double discount;
    private String notes;
}