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

    public UUID getSyncDeviceStateId() { return syncDeviceStateId; }
    public void setSyncDeviceStateId(UUID syncDeviceStateId) { this.syncDeviceStateId = syncDeviceStateId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getLastPulledAt() { return lastPulledAt; }
    public void setLastPulledAt(LocalDateTime lastPulledAt) { this.lastPulledAt = lastPulledAt; }

    public LocalDateTime getLastPushedAt() { return lastPushedAt; }
    public void setLastPushedAt(LocalDateTime lastPushedAt) { this.lastPushedAt = lastPushedAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Integer getConflictCount() { return conflictCount; }
    public void setConflictCount(Integer conflictCount) { this.conflictCount = conflictCount; }

    public static SyncDeviceStateBuilder builder() { return new SyncDeviceStateBuilder(); }

    public static class SyncDeviceStateBuilder {
        private UUID syncDeviceStateId;
        private User user;
        private String deviceId;
        private LocalDateTime lastPulledAt;
        private LocalDateTime lastPushedAt;
        private LocalDateTime lastSeenAt;
        private Integer conflictCount = 0;

        public SyncDeviceStateBuilder syncDeviceStateId(UUID syncDeviceStateId) { this.syncDeviceStateId = syncDeviceStateId; return this; }
        public SyncDeviceStateBuilder user(User user) { this.user = user; return this; }
        public SyncDeviceStateBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public SyncDeviceStateBuilder lastPulledAt(LocalDateTime lastPulledAt) { this.lastPulledAt = lastPulledAt; return this; }
        public SyncDeviceStateBuilder lastPushedAt(LocalDateTime lastPushedAt) { this.lastPushedAt = lastPushedAt; return this; }
        public SyncDeviceStateBuilder lastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; return this; }
        public SyncDeviceStateBuilder conflictCount(Integer conflictCount) { this.conflictCount = conflictCount; return this; }

        public SyncDeviceState build() {
            SyncDeviceState state = new SyncDeviceState();
            state.syncDeviceStateId = this.syncDeviceStateId;
            state.user = this.user;
            state.deviceId = this.deviceId;
            state.lastPulledAt = this.lastPulledAt;
            state.lastPushedAt = this.lastPushedAt;
            state.lastSeenAt = this.lastSeenAt;
            state.conflictCount = this.conflictCount;
            return state;
        }
    }
}
