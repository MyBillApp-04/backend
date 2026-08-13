package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_ledger_entries", indexes = {
        @Index(name = "idx_client_ledger_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_client_ledger_client_date", columnList = "client_id, transaction_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLedgerEntry {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerDirection direction;

    @Column(nullable = false, columnDefinition = "double precision")
    private Double amount;

    @Column(nullable = false, columnDefinition = "double precision")
    private Double balanceAfter;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Column(name = "change_log", columnDefinition = "text")
    private String changeLog;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    private String deviceId;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID();
        if (amount == null) amount = 0.0;
        if (balanceAfter == null) balanceAfter = 0.0;
        if (transactionDate == null) transactionDate = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isDeleted == null) isDeleted = false;
        if (version == null) version = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        version = version == null ? 1 : version + 1;
    }

    public void markDeleted(LocalDateTime deletedAt) {
        LocalDateTime timestamp = deletedAt != null ? deletedAt : LocalDateTime.now();
        this.isDeleted = true;
        this.deletedAt = timestamp;
        this.updatedAt = timestamp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LedgerEntryType getType() { return type; }
    public void setType(LedgerEntryType type) { this.type = type; }
    public LedgerDirection getDirection() { return direction; }
    public void setDirection(LedgerDirection direction) { this.direction = direction; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getChangeLog() { return changeLog; }
    public void setChangeLog(String changeLog) { this.changeLog = changeLog; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public static ClientLedgerEntryBuilder builder() { return new ClientLedgerEntryBuilder(); }

    public static class ClientLedgerEntryBuilder {
        private UUID id;
        private Client client;
        private Invoice invoice;
        private Payment payment;
        private User user;
        private LedgerEntryType type;
        private LedgerDirection direction;
        private Double amount;
        private Double balanceAfter;
        private String notes;
        private LocalDateTime transactionDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private String changeLog;
        private Boolean isDeleted = false;
        private String deviceId;
        private Integer version = 1;
        private Long createdBy;
        private Long updatedBy;

        public ClientLedgerEntryBuilder id(UUID id) { this.id = id; return this; }
        public ClientLedgerEntryBuilder client(Client client) { this.client = client; return this; }
        public ClientLedgerEntryBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public ClientLedgerEntryBuilder payment(Payment payment) { this.payment = payment; return this; }
        public ClientLedgerEntryBuilder user(User user) { this.user = user; return this; }
        public ClientLedgerEntryBuilder type(LedgerEntryType type) { this.type = type; return this; }
        public ClientLedgerEntryBuilder direction(LedgerDirection direction) { this.direction = direction; return this; }
        public ClientLedgerEntryBuilder amount(Double amount) { this.amount = amount; return this; }
        public ClientLedgerEntryBuilder balanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public ClientLedgerEntryBuilder notes(String notes) { this.notes = notes; return this; }
        public ClientLedgerEntryBuilder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public ClientLedgerEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ClientLedgerEntryBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ClientLedgerEntryBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public ClientLedgerEntryBuilder changeLog(String changeLog) { this.changeLog = changeLog; return this; }
        public ClientLedgerEntryBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public ClientLedgerEntryBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public ClientLedgerEntryBuilder version(Integer version) { this.version = version; return this; }
        public ClientLedgerEntryBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public ClientLedgerEntryBuilder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }

        public ClientLedgerEntry build() {
            ClientLedgerEntry entry = new ClientLedgerEntry();
            entry.id = this.id;
            entry.client = this.client;
            entry.invoice = this.invoice;
            entry.payment = this.payment;
            entry.user = this.user;
            entry.type = this.type;
            entry.direction = this.direction;
            entry.amount = this.amount;
            entry.balanceAfter = this.balanceAfter;
            entry.notes = this.notes;
            entry.transactionDate = this.transactionDate;
            entry.createdAt = this.createdAt;
            entry.updatedAt = this.updatedAt;
            entry.deletedAt = this.deletedAt;
            entry.changeLog = this.changeLog;
            entry.isDeleted = this.isDeleted;
            entry.deviceId = this.deviceId;
            entry.version = this.version;
            entry.createdBy = this.createdBy;
            entry.updatedBy = this.updatedBy;
            return entry;
        }
    }
}
