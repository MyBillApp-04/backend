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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_client", columnList = "client_id"),
        @Index(name = "idx_payments_user_date", columnList = "user_id, date"),
        @Index(name = "idx_payments_user_deleted", columnList = "user_id, is_deleted")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(name = "payment_id", columnDefinition = "uuid")
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private PaymentMode paymentMode;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(columnDefinition = "text")
    private String notes;

    private String stripePaymentIntentId;
    private String stripeChargeId;
    private String stripeRefundId;
    private String stripeStatus;
    private Double refundedAmount;

    @Builder.Default
    @Column(nullable = false)
    private Boolean appliedToInvoice = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (paymentId == null) paymentId = UUID.randomUUID();
        if (date == null) date = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isDeleted == null) isDeleted = false;
        if (appliedToInvoice == null) appliedToInvoice = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markDeleted(LocalDateTime deletedAt) {
        LocalDateTime timestamp = deletedAt != null ? deletedAt : LocalDateTime.now();
        this.isDeleted = true;
        this.deletedAt = timestamp;
        this.updatedAt = timestamp;
    }
}
