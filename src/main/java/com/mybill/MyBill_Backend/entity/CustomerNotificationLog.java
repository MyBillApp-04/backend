package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_notification_logs", indexes = {
        @Index(name = "idx_cust_notification_logs_user_status", columnList = "user_id, status"),
        @Index(name = "idx_cust_notification_logs_created", columnList = "created_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNotificationLog {

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Client customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status; // PENDING, SENT, FAILED, RETRYING

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "provider_response", columnDefinition = "text")
    private String providerResponse;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (notificationId == null) notificationId = UUID.randomUUID();
        if (createdTime == null) createdTime = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (retryCount == null) retryCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
