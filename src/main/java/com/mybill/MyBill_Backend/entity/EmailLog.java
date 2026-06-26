package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_logs", indexes = {
        @Index(name = "idx_email_logs_user_status", columnList = "user_id, status"),
        @Index(name = "idx_email_logs_next_retry", columnList = "next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @Column(name = "email_log_id", columnDefinition = "uuid")
    private UUID emailLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String recipient;
    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    private String templateType;
    private UUID invoiceId;
    private Integer attemptCount;
    private LocalDateTime sentAt;
    private LocalDateTime nextRetryAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (emailLogId == null) emailLogId = UUID.randomUUID();
        if (status == null) status = EmailStatus.PENDING;
        if (attemptCount == null) attemptCount = 0;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
