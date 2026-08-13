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

    /** State (Indian state name or code) used for intra/inter-state GST determination. */
    private String state;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getQrImagePath() { return qrImagePath; }
    public void setQrImagePath(String qrImagePath) { this.qrImagePath = qrImagePath; }
    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }
    public String getLogoPublicId() { return logoPublicId; }
    public void setLogoPublicId(String logoPublicId) { this.logoPublicId = logoPublicId; }
    public String getLogoResourceType() { return logoResourceType; }
    public void setLogoResourceType(String logoResourceType) { this.logoResourceType = logoResourceType; }
    public Integer getLogoWidth() { return logoWidth; }
    public void setLogoWidth(Integer logoWidth) { this.logoWidth = logoWidth; }
    public Integer getLogoHeight() { return logoHeight; }
    public void setLogoHeight(Integer logoHeight) { this.logoHeight = logoHeight; }
    public String getLogoFormat() { return logoFormat; }
    public void setLogoFormat(String logoFormat) { this.logoFormat = logoFormat; }
    public String getQrImagePublicId() { return qrImagePublicId; }
    public void setQrImagePublicId(String qrImagePublicId) { this.qrImagePublicId = qrImagePublicId; }
    public String getQrImageResourceType() { return qrImageResourceType; }
    public void setQrImageResourceType(String qrImageResourceType) { this.qrImageResourceType = qrImageResourceType; }
    public Integer getQrImageWidth() { return qrImageWidth; }
    public void setQrImageWidth(Integer qrImageWidth) { this.qrImageWidth = qrImageWidth; }
    public Integer getQrImageHeight() { return qrImageHeight; }
    public void setQrImageHeight(Integer qrImageHeight) { this.qrImageHeight = qrImageHeight; }
    public String getQrImageFormat() { return qrImageFormat; }
    public void setQrImageFormat(String qrImageFormat) { this.qrImageFormat = qrImageFormat; }
    public String getSignaturePublicId() { return signaturePublicId; }
    public void setSignaturePublicId(String signaturePublicId) { this.signaturePublicId = signaturePublicId; }
    public String getSignatureResourceType() { return signatureResourceType; }
    public void setSignatureResourceType(String signatureResourceType) { this.signatureResourceType = signatureResourceType; }
    public Integer getSignatureWidth() { return signatureWidth; }
    public void setSignatureWidth(Integer signatureWidth) { this.signatureWidth = signatureWidth; }
    public Integer getSignatureHeight() { return signatureHeight; }
    public void setSignatureHeight(Integer signatureHeight) { this.signatureHeight = signatureHeight; }
    public String getSignatureFormat() { return signatureFormat; }
    public void setSignatureFormat(String signatureFormat) { this.signatureFormat = signatureFormat; }
    public String getThankYouNote() { return thankYouNote; }
    public void setThankYouNote(String thankYouNote) { this.thankYouNote = thankYouNote; }
    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }
    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }
    public Integer getNextInvoiceNumber() { return nextInvoiceNumber; }
    public void setNextInvoiceNumber(Integer nextInvoiceNumber) { this.nextInvoiceNumber = nextInvoiceNumber; }
    public Boolean getFinancialYearEnabled() { return financialYearEnabled; }
    public void setFinancialYearEnabled(Boolean financialYearEnabled) { this.financialYearEnabled = financialYearEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
