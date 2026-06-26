package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceFilterDTO {
    private String query;
    private UUID clientId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<PaymentStatus> statuses;
    private Double minAmount;
    private Double maxAmount;
}
