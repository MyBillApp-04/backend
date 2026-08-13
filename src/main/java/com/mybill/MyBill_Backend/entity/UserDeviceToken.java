package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_device_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceToken {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(nullable = false, length = 20)
    private String platform; // ANDROID, IOS, WEB, UNKNOWN

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.platform == null) {
            this.platform = "UNKNOWN";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static UserDeviceTokenBuilder builder() { return new UserDeviceTokenBuilder(); }

    public static class UserDeviceTokenBuilder {
        private UUID id;
        private User user;
        private String fcmToken;
        private String platform = "UNKNOWN";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserDeviceTokenBuilder id(UUID id) { this.id = id; return this; }
        public UserDeviceTokenBuilder user(User user) { this.user = user; return this; }
        public UserDeviceTokenBuilder fcmToken(String fcmToken) { this.fcmToken = fcmToken; return this; }
        public UserDeviceTokenBuilder platform(String platform) { this.platform = platform; return this; }
        public UserDeviceTokenBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserDeviceTokenBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserDeviceToken build() {
            UserDeviceToken t = new UserDeviceToken();
            t.id = this.id;
            t.user = this.user;
            t.fcmToken = this.fcmToken;
            t.platform = this.platform;
            t.createdAt = this.createdAt;
            t.updatedAt = this.updatedAt;
            return t;
        }
    }
}
