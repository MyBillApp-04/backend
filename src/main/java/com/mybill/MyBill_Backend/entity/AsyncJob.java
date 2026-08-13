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

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static AsyncJobBuilder builder() { return new AsyncJobBuilder(); }

    public static class AsyncJobBuilder {
        private UUID jobId;
        private String jobType;
        private String payload;
        private String status = "PENDING";
        private Integer attemptCount = 0;
        private Integer maxAttempts = 5;
        private LocalDateTime nextRunAt;
        private String lastError;
        private User user;
        private UUID invoiceId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AsyncJobBuilder jobId(UUID jobId) { this.jobId = jobId; return this; }
        public AsyncJobBuilder jobType(String jobType) { this.jobType = jobType; return this; }
        public AsyncJobBuilder payload(String payload) { this.payload = payload; return this; }
        public AsyncJobBuilder status(String status) { this.status = status; return this; }
        public AsyncJobBuilder attemptCount(Integer attemptCount) { this.attemptCount = attemptCount; return this; }
        public AsyncJobBuilder maxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public AsyncJobBuilder nextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; return this; }
        public AsyncJobBuilder lastError(String lastError) { this.lastError = lastError; return this; }
        public AsyncJobBuilder user(User user) { this.user = user; return this; }
        public AsyncJobBuilder invoiceId(UUID invoiceId) { this.invoiceId = invoiceId; return this; }
        public AsyncJobBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AsyncJobBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AsyncJob build() {
            AsyncJob job = new AsyncJob();
            job.jobId = this.jobId;
            job.jobType = this.jobType;
            job.payload = this.payload;
            job.status = this.status;
            job.attemptCount = this.attemptCount;
            job.maxAttempts = this.maxAttempts;
            job.nextRunAt = this.nextRunAt;
            job.lastError = this.lastError;
            job.user = this.user;
            job.invoiceId = this.invoiceId;
            job.createdAt = this.createdAt;
            job.updatedAt = this.updatedAt;
            return job;
        }
    }
}
