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

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double balanceAfter;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    private String deviceId;

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
}
