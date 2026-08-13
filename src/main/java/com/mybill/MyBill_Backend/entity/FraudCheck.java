package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_checks", indexes = {
        @Index(name = "idx_fraud_checks_payment", columnList = "payment_id"),
        @Index(name = "idx_fraud_checks_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheck {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "double precision")
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudStatus status;

    @Column(name = "rules_triggered")
    private String rulesTriggered;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public FraudStatus getStatus() { return status; }
    public void setStatus(FraudStatus status) { this.status = status; }
    public String getRulesTriggered() { return rulesTriggered; }
    public void setRulesTriggered(String rulesTriggered) { this.rulesTriggered = rulesTriggered; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public static FraudCheckBuilder builder() { return new FraudCheckBuilder(); }

    public static class FraudCheckBuilder {
        private UUID id;
        private Payment payment;
        private User user;
        private Double score;
        private FraudStatus status;
        private String rulesTriggered;
        private String notes;
        private LocalDateTime createdAt;
        private Long createdBy;

        public FraudCheckBuilder id(UUID id) { this.id = id; return this; }
        public FraudCheckBuilder payment(Payment payment) { this.payment = payment; return this; }
        public FraudCheckBuilder user(User user) { this.user = user; return this; }
        public FraudCheckBuilder score(Double score) { this.score = score; return this; }
        public FraudCheckBuilder status(FraudStatus status) { this.status = status; return this; }
        public FraudCheckBuilder rulesTriggered(String rulesTriggered) { this.rulesTriggered = rulesTriggered; return this; }
        public FraudCheckBuilder notes(String notes) { this.notes = notes; return this; }
        public FraudCheckBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public FraudCheckBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }

        public FraudCheck build() {
            FraudCheck f = new FraudCheck();
            f.id = this.id;
            f.payment = this.payment;
            f.user = this.user;
            f.score = this.score;
            f.status = this.status;
            f.rulesTriggered = this.rulesTriggered;
            f.notes = this.notes;
            f.createdAt = this.createdAt;
            f.createdBy = this.createdBy;
            return f;
        }
    }
}
