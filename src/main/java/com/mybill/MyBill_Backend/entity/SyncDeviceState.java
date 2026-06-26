package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_device_state", indexes = {
        @Index(name = "idx_sync_device_user_device", columnList = "user_id, device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncDeviceState {

    @Id
    @Column(name = "sync_device_state_id", columnDefinition = "uuid")
    private UUID syncDeviceStateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    private LocalDateTime lastPulledAt;
    private LocalDateTime lastPushedAt;
    private LocalDateTime lastSeenAt;
    private Integer conflictCount;

    @PrePersist
    public void prePersist() {
        if (syncDeviceStateId == null) syncDeviceStateId = UUID.randomUUID();
        if (lastSeenAt == null) lastSeenAt = LocalDateTime.now();
        if (conflictCount == null) conflictCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        lastSeenAt = LocalDateTime.now();
    }
}
