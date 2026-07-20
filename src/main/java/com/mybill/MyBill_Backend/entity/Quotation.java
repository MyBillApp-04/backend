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
        name = "quotation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quotation_user_number",
                columnNames = {"user_id", "quotation_number"}
        ),
        indexes = {
                @Index(name = "idx_quotation_user_updated", columnList = "user_id, updated_at"),
                @Index(name = "idx_quotation_user_deleted", columnList = "user_id, is_deleted"),
                @Index(name = "idx_quotation_client", columnList = "client_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quotation_number", nullable = false)
    private String quotationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "valid_until_date")
    private LocalDateTime validUntilDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "terms_and_conditions", columnDefinition = "text")
    private String termsAndConditions;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(columnDefinition = "double precision")
    private Double subtotal;

    @Column(columnDefinition = "double precision")
    private Double discount;

    @Column(name = "gross_amount", columnDefinition = "double precision")
    private Double grossAmount;

    @Column(name = "total_amount", columnDefinition = "double precision")
    private Double totalAmount;

    @Column(name = "net_payable", columnDefinition = "double precision")
    private Double netPayable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "device_id")
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
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.issueDate == null) this.issueDate = now;
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.version == null) this.version = 1;

        if (this.subtotal == null) this.subtotal = 0.0;
        if (this.discount == null) this.discount = 0.0;
        if (this.grossAmount == null) this.grossAmount = this.subtotal;
        if (this.totalAmount == null) this.totalAmount = 0.0;
        if (this.netPayable == null) this.netPayable = this.totalAmount;
        if (this.status == null) this.status = QuotationStatus.DRAFT;
    }

    @PreUpdate
    public void preUpdate() {
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
