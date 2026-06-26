package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "invoice",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_invoice_user_number",
                columnNames = {"user_id", "invoice_number"}
        ),
        indexes = {
                @Index(name = "idx_invoice_user_updated", columnList = "user_id, updated_at"),
                @Index(name = "idx_invoice_user_deleted", columnList = "user_id, is_deleted"),
                @Index(name = "idx_invoice_client", columnList = "client_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String invoiceNumber;

    /** Null for invoices issued before the FY-based numbering rollout. */
    @Column(name = "financial_year", length = 9)
    private String financialYear;

    /** Null for legacy invoices; new invoices are allocated globally per FY. */
    @Column(name = "sequence_no")
    private Integer sequenceNo;

    // Financial Math
    private Double subtotal;
    private Double discount;
    private Double grossAmount;
    private Double advanceApplied;
    private Double netPayable;
    private Double totalAmount;

    // Payment Tracking
    private Double paidAmount;
    private Double pendingAmount;
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    // Dates
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;
    private LocalDateTime paymentDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // PDF & Notes
    @Column(columnDefinition = "text")
    private String notes;
    private String pdfUrl;
    private String pdfPath;

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

    @Builder.Default
    @OneToMany(mappedBy = "invoice")
    private List<InvoiceItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdDate == null) this.createdDate = now;
        if (this.invoiceDate == null) this.invoiceDate = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.version == null) this.version = 1;

        // Default Financial & Payment Logic
        if (this.subtotal == null) this.subtotal = 0.0;
        if (this.discount == null) this.discount = 0.0;
        if (this.grossAmount == null) this.grossAmount = this.subtotal;
        if (this.advanceApplied == null) this.advanceApplied = 0.0;
        if (this.totalAmount == null) this.totalAmount = 0.0;
        if (this.netPayable == null) this.netPayable = this.totalAmount;
        if (this.paidAmount == null) this.paidAmount = 0.0;
        if (this.pendingAmount == null) this.pendingAmount = this.totalAmount - this.paidAmount;
        if (this.remainingAmount == null) this.remainingAmount = this.pendingAmount;
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.UNPAID;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.version == null) {
            this.version = 1;
        } else {
            this.version++;
        }

        // Recalculate pending amount on update just to be safe
        if (this.totalAmount != null && this.paidAmount != null) {
            this.pendingAmount = this.totalAmount - this.paidAmount;
            this.remainingAmount = Math.max(this.pendingAmount, 0.0);

            // Auto-update status based on math
            if (this.totalAmount <= 0 || this.paidAmount >= this.totalAmount) {
                this.paymentStatus = PaymentStatus.PAID;
            } else if (this.paidAmount > 0) {
                this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
            } else {
                this.paymentStatus = PaymentStatus.UNPAID;
            }
        }
    }

    public void markDeleted(LocalDateTime deletedAt) {
        LocalDateTime timestamp = deletedAt != null ? deletedAt : LocalDateTime.now();
        this.isDeleted = true;
        this.deletedAt = timestamp;
        this.updatedAt = timestamp;
    }
}
