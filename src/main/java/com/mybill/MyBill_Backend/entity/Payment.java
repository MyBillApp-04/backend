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

    @Column(nullable = false, columnDefinition = "double precision")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private PaymentMode paymentMode;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "double precision")
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

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (paymentId == null) paymentId = UUID.randomUUID();
        if (date == null) date = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isDeleted == null) isDeleted = false;
        if (appliedToInvoice == null) appliedToInvoice = false;
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

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Double getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; }
    public Boolean getAppliedToInvoice() { return appliedToInvoice; }
    public void setAppliedToInvoice(Boolean appliedToInvoice) { this.appliedToInvoice = appliedToInvoice; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public static PaymentBuilder builder() { return new PaymentBuilder(); }

    public static class PaymentBuilder {
        private UUID paymentId;
        private Client client;
        private Invoice invoice;
        private User user;
        private Double amount;
        private PaymentMode paymentMode;
        private LocalDateTime date;
        private String notes;
        private Double refundedAmount = 0.0;
        private Boolean appliedToInvoice = false;
        private Boolean isDeleted = false;
        private LocalDateTime deletedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long createdBy;
        private Long updatedBy;
        private Integer version = 1;

        public PaymentBuilder paymentId(UUID paymentId) { this.paymentId = paymentId; return this; }
        public PaymentBuilder client(Client client) { this.client = client; return this; }
        public PaymentBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public PaymentBuilder user(User user) { this.user = user; return this; }
        public PaymentBuilder amount(Double amount) { this.amount = amount; return this; }
        public PaymentBuilder paymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; return this; }
        public PaymentBuilder date(LocalDateTime date) { this.date = date; return this; }
        public PaymentBuilder notes(String notes) { this.notes = notes; return this; }
        public PaymentBuilder refundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; return this; }
        public PaymentBuilder appliedToInvoice(Boolean appliedToInvoice) { this.appliedToInvoice = appliedToInvoice; return this; }
        public PaymentBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public PaymentBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public PaymentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PaymentBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public PaymentBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public PaymentBuilder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public PaymentBuilder version(Integer version) { this.version = version; return this; }

        public Payment build() {
            Payment p = new Payment();
            p.paymentId = this.paymentId;
            p.client = this.client;
            p.invoice = this.invoice;
            p.user = this.user;
            p.amount = this.amount;
            p.paymentMode = this.paymentMode;
            p.date = this.date;
            p.notes = this.notes;
            p.refundedAmount = this.refundedAmount;
            p.appliedToInvoice = this.appliedToInvoice;
            p.isDeleted = this.isDeleted;
            p.deletedAt = this.deletedAt;
            p.createdAt = this.createdAt;
            p.updatedAt = this.updatedAt;
            p.createdBy = this.createdBy;
            p.updatedBy = this.updatedBy;
            p.version = this.version;
            return p;
        }
    }
}
