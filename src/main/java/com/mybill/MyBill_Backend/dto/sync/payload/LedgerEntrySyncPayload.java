package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntrySyncPayload {
    private String id;
    private String clientId;
    private String invoiceId;
    private String paymentId;
    private String type;
    private String direction;
    private Double amount;
    private Double balanceAfter;
    private String notes;
    private LocalDateTime transactionDate;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private String deviceId;
    private String userKey;
}
