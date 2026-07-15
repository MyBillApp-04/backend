package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "business_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_business_profile_user",
                columnNames = "user_id"
        )
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Core business info
    private String businessName;
    private String ownerName;
    private String address;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone must be a 10-digit number")
    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    // Compliance
    @Pattern(
            regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "GSTIN must be valid"
    )
    private String gstin;

    // Bank / payment details
    private String bankName;
    private String accountNumber;
    private String ifsc;
    private String upiId;

    // Image references returned to clients. New uploads use Cloudinary URLs and metadata.
    private String logoPath;
    private String qrImagePath;
    private String signaturePath;
    private String logoPublicId;
    private String logoResourceType;
    private Integer logoWidth;
    private Integer logoHeight;
    private String logoFormat;
    private String qrImagePublicId;
    private String qrImageResourceType;
    private Integer qrImageWidth;
    private Integer qrImageHeight;
    private String qrImageFormat;
    private String signaturePublicId;
    private String signatureResourceType;
    private Integer signatureWidth;
    private Integer signatureHeight;
    private String signatureFormat;

    // Invoice customization & Settings
    @Column(columnDefinition = "text")
    private String thankYouNote;

    @Column(columnDefinition = "text")
    private String termsAndConditions;

    private String invoicePrefix;
    private Integer nextInvoiceNumber;
    private Boolean financialYearEnabled;

    // Audit timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;

        // Default Invoice Settings
        if (invoicePrefix == null) invoicePrefix = "INV";
        if (nextInvoiceNumber == null) nextInvoiceNumber = 1;
        if (financialYearEnabled == null) financialYearEnabled = false;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
