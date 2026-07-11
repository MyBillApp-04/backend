package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.mybill.MyBill_Backend.validation.TwoDecimalPlaces;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_work", indexes = {
        @Index(name = "idx_work_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_work_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_work_client", columnList = "client_id"),
        @Index(name = "idx_work_invoice", columnList = "invoice_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWork {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotBlank(message = "Work description is required")
    private String description;

    @NotNull(message = "Rate is required")
    @Positive(message = "Rate must be positive")
    @TwoDecimalPlaces(message = "Rate can have at most two decimal places")
    @Column(columnDefinition = "double precision")
    private Double rate;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Column(columnDefinition = "double precision")
    private Double amount;

    @PastOrPresent(message = "Date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean billed = false;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonBackReference
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (quantity != null && rate != null) {
            this.amount = quantity * rate;
        }

        if (this.date == null) {
            this.date = now;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.billed == null) {
            this.billed = false;
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
        if (quantity != null && rate != null) {
            this.amount = quantity * rate;
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
