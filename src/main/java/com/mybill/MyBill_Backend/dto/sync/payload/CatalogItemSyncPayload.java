package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogItemSyncPayload {

    private String id;
    private String userId;

    private String name;
    private String description;
    private String type; // PRODUCT, SERVICE

    private Double defaultRate;
    private Double defaultTaxRate;

    private String unit;
    private String dimension;
    private Double kgs;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Boolean isDeleted;

    private String deviceId;
    private String userKey;
    private Integer version;
}