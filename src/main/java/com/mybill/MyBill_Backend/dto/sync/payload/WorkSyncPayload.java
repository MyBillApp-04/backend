package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSyncPayload {

    private String id;
    private String clientId;
    private String invoiceId;

    private String description;
    private Double rate;
    private Integer quantity;
    private Double amount;

    private LocalDateTime workDate;
    private LocalDateTime date;

    private Boolean billed;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;

    public LocalDateTime getWorkDate() {
        return workDate != null ? workDate : date;
    }
}
