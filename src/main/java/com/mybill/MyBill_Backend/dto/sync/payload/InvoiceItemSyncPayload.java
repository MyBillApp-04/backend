package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemSyncPayload {

    private String id;
    private String invoiceId;
    private String workId;

    private String description;
    private Double rate;
    private Integer quantity;
    private Double amount;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;
}
