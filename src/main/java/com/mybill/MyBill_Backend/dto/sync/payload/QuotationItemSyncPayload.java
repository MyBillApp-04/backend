package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationItemSyncPayload {

    private String id;
    private String quotationId;

    private String description;
    private String dimension;
    private Integer quantity;
    private Double kgs;
    private Double amount;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;
}
