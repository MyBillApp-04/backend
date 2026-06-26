package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSyncPayload {

    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;

    // Added to resolve optimistic locking getVersion() errors
    private Integer version;
}
