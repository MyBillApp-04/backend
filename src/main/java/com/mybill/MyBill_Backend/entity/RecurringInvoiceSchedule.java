package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_invoice_schedule", indexes = {
        @Index(name = "idx_recurring_invoice_schedule_user", columnList = "user_id, status"),
        @Index(name = "idx_recurring_invoice_schedule_run", columnList = "status, next_run_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceSchedule {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle; // e.g. WEEKLY, MONTHLY, QUARTERLY, YEARLY

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, PAUSED, EXPIRED

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDateTime nextRunDate;

    @Column(name = "last_run_date")
    private LocalDateTime lastRunDate;

    @Builder.Default
    @Column(name = "auto_charge", nullable = false)
    private Boolean autoCharge = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; }

    public LocalDateTime getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(LocalDateTime lastRunDate) { this.lastRunDate = lastRunDate; }

    public Boolean getAutoCharge() { return autoCharge; }
    public void setAutoCharge(Boolean autoCharge) { this.autoCharge = autoCharge; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public static RecurringInvoiceScheduleBuilder builder() { return new RecurringInvoiceScheduleBuilder(); }

    public static class RecurringInvoiceScheduleBuilder {
        private UUID id;
        private Client client;
        private User user;
        private String description;
        private BigDecimal amount;
        private String billingCycle;
        private String cronExpression;
        private String status = "ACTIVE";
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime nextRunDate;
        private LocalDateTime lastRunDate;
        private Boolean autoCharge = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean isDeleted = false;
        private LocalDateTime deletedAt;
        private Integer version = 1;

        public RecurringInvoiceScheduleBuilder id(UUID id) { this.id = id; return this; }
        public RecurringInvoiceScheduleBuilder client(Client client) { this.client = client; return this; }
        public RecurringInvoiceScheduleBuilder user(User user) { this.user = user; return this; }
        public RecurringInvoiceScheduleBuilder description(String description) { this.description = description; return this; }
        public RecurringInvoiceScheduleBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public RecurringInvoiceScheduleBuilder billingCycle(String billingCycle) { this.billingCycle = billingCycle; return this; }
        public RecurringInvoiceScheduleBuilder cronExpression(String cronExpression) { this.cronExpression = cronExpression; return this; }
        public RecurringInvoiceScheduleBuilder status(String status) { this.status = status; return this; }
        public RecurringInvoiceScheduleBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public RecurringInvoiceScheduleBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public RecurringInvoiceScheduleBuilder nextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; return this; }
        public RecurringInvoiceScheduleBuilder lastRunDate(LocalDateTime lastRunDate) { this.lastRunDate = lastRunDate; return this; }
        public RecurringInvoiceScheduleBuilder autoCharge(Boolean autoCharge) { this.autoCharge = autoCharge; return this; }
        public RecurringInvoiceScheduleBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RecurringInvoiceScheduleBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RecurringInvoiceScheduleBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public RecurringInvoiceScheduleBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public RecurringInvoiceScheduleBuilder version(Integer version) { this.version = version; return this; }

        public RecurringInvoiceSchedule build() {
            RecurringInvoiceSchedule s = new RecurringInvoiceSchedule();
            s.id = this.id;
            s.client = this.client;
            s.user = this.user;
            s.description = this.description;
            s.amount = this.amount;
            s.billingCycle = this.billingCycle;
            s.cronExpression = this.cronExpression;
            s.status = this.status;
            s.startDate = this.startDate;
            s.endDate = this.endDate;
            s.nextRunDate = this.nextRunDate;
            s.lastRunDate = this.lastRunDate;
            s.autoCharge = this.autoCharge;
            s.createdAt = this.createdAt;
            s.updatedAt = this.updatedAt;
            s.isDeleted = this.isDeleted;
            s.deletedAt = this.deletedAt;
            s.version = this.version;
            return s;
        }
    }
}
