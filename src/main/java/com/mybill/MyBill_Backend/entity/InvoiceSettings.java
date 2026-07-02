package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSettings {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "invoice_prefix", length = 20)
    private String invoicePrefix;

    @Column(name = "next_invoice_number")
    private Integer nextInvoiceNumber;

    @Column(name = "default_due_days")
    private Integer defaultDueDays;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "payment_note", columnDefinition = "TEXT")
    private String paymentNote;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "template_style", length = 50)
    @Builder.Default
    private String templateStyle = "CLASSIC";

    @Column(name = "theme_color", length = 20)
    @Builder.Default
    private String themeColor = "#225378";

    @Column(name = "font_family", length = 50)
    @Builder.Default
    private String fontFamily = "HELVETICA";

    @Column(name = "show_logo")
    @Builder.Default
    private Boolean showLogo = true;

    @Column(name = "tax_id_label", length = 50)
    @Builder.Default
    private String taxIdLabel = "";

    @Column(name = "tax_id_value", length = 100)
    @Builder.Default
    private String taxIdValue = "";

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
