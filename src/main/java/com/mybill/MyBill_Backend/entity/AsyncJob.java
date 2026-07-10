package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "async_jobs", indexes = {
        @Index(name = "idx_async_jobs_status_next_run", columnList = "status, next_run_at"),
        @Index(name = "idx_async_jobs_user_invoice", columnList = "user_id, invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncJob {

    @Id
    @Column(name = "job_id", columnDefinition = "uuid")
    private UUID jobId;

    @Column(name = "job_type", length = 80, nullable = false)
    private String jobType;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(length = 30, nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (jobId == null) jobId = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (attemptCount == null) attemptCount = 0;
        if (maxAttempts == null) maxAttempts = 5;
        if (nextRunAt == null) nextRunAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
