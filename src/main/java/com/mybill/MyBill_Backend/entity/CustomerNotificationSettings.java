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
@Table(name = "customer_notification_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNotificationSettings {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @JsonIgnore
    private User user;

    @Builder.Default
    @Column(name = "enable_whatsapp", nullable = false)
    private Boolean enableWhatsApp = true;

    @Builder.Default
    @Column(name = "enable_invoice_generated", nullable = false)
    private Boolean enableInvoiceGenerated = true;

    @Builder.Default
    @Column(name = "enable_payment_received", nullable = false)
    private Boolean enablePaymentReceived = true;

    @Builder.Default
    @Column(name = "enable_partial_payment", nullable = false)
    private Boolean enablePartialPayment = true;

    @Builder.Default
    @Column(name = "enable_invoice_updated", nullable = false)
    private Boolean enableInvoiceUpdated = true;

    @Builder.Default
    @Column(name = "enable_advance_balance", nullable = false)
    private Boolean enableAdvanceBalance = true;

    @Builder.Default
    @Column(name = "enable_payment_reminder", nullable = false)
    private Boolean enablePaymentReminder = true;

    @Builder.Default
    @Column(name = "reminder_frequency_days", nullable = false)
    private Integer reminderFrequencyDays = 7;

    @Builder.Default
    @Column(name = "enable_powered_by_mybill", nullable = false)
    private Boolean enablePoweredByMyBill = true;

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
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (enableWhatsApp == null) enableWhatsApp = true;
        if (enableInvoiceGenerated == null) enableInvoiceGenerated = true;
        if (enablePaymentReceived == null) enablePaymentReceived = true;
        if (enablePartialPayment == null) enablePartialPayment = true;
        if (enableInvoiceUpdated == null) enableInvoiceUpdated = true;
        if (enableAdvanceBalance == null) enableAdvanceBalance = true;
        if (enablePaymentReminder == null) enablePaymentReminder = true;
        if (reminderFrequencyDays == null) reminderFrequencyDays = 7;
        if (enablePoweredByMyBill == null) enablePoweredByMyBill = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
