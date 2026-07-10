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
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_items_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_invoice_items_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_invoice_items_invoice", columnList = "invoice_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private ClientWork work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String description;
    @Column(columnDefinition = "double precision")
    private Double rate;
    private Integer quantity;
    @Column(columnDefinition = "double precision")
    private Double amount;

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

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.quantity != null && this.rate != null) {
            this.amount = this.quantity * this.rate;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.isDeleted == null) {
            this.isDeleted = false;
        }

        if (this.version == null) {
            this.version = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.quantity != null && this.rate != null) {
            this.amount = this.quantity * this.rate;
        }

        this.updatedAt = LocalDateTime.now();

        if (this.version == null) {
            this.version = 1;
        } else {
            this.version++;
        }
    }

    public void markDeleted(LocalDateTime deletedAt) {
        LocalDateTime timestamp = deletedAt != null ? deletedAt : LocalDateTime.now();
        this.isDeleted = true;
        this.deletedAt = timestamp;
        this.updatedAt = timestamp;
    }
}
