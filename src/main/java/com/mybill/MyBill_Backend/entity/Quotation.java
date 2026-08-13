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

    @Column(name = "public_token_hash", length = 64)
    private String publicTokenHash;

    /** Raw URL-safe base64 token (43 chars). Stored alongside the hash so the
     *  sync pull endpoint can echo it back to clients for link reconstruction.
     *  Never used for authentication lookups — only the hash is used for that. */
    @Column(name = "public_token", length = 43)
    private String publicToken;

    @Column(name = "token_created_at")
    private LocalDateTime tokenCreatedAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "token_revoked_at")
    private LocalDateTime tokenRevokedAt;

    @Column(name = "client_response_status", length = 30)
    private String clientResponseStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "discussion_message", columnDefinition = "text")
    private String discussionMessage;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getQuotationNumber() { return quotationNumber; }
    public void setQuotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; }

    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }

    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }

    public LocalDateTime getValidUntilDate() { return validUntilDate; }
    public void setValidUntilDate(LocalDateTime validUntilDate) { this.validUntilDate = validUntilDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(Double grossAmount) { this.grossAmount = grossAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getNetPayable() { return netPayable; }
    public void setNetPayable(Double netPayable) { this.netPayable = netPayable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getPublicTokenHash() { return publicTokenHash; }
    public void setPublicTokenHash(String publicTokenHash) { this.publicTokenHash = publicTokenHash; }

    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }

    public LocalDateTime getTokenCreatedAt() { return tokenCreatedAt; }
    public void setTokenCreatedAt(LocalDateTime tokenCreatedAt) { this.tokenCreatedAt = tokenCreatedAt; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public LocalDateTime getTokenRevokedAt() { return tokenRevokedAt; }
    public void setTokenRevokedAt(LocalDateTime tokenRevokedAt) { this.tokenRevokedAt = tokenRevokedAt; }

    public String getClientResponseStatus() { return clientResponseStatus; }
    public void setClientResponseStatus(String clientResponseStatus) { this.clientResponseStatus = clientResponseStatus; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getDiscussionMessage() { return discussionMessage; }
    public void setDiscussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public List<QuotationItem> getItems() { return items; }
    public void setItems(List<QuotationItem> items) { this.items = items; }

    public static QuotationBuilder builder() { return new QuotationBuilder(); }

    public static class QuotationBuilder {
        private UUID id;
        private Client client;
        private User user;
        private String quotationNumber;
        private QuotationStatus status = QuotationStatus.DRAFT;
        private LocalDateTime issueDate;
        private LocalDateTime validUntilDate;
        private String notes;
        private String termsAndConditions;
        private String pdfUrl;
        private String pdfPath;
        private Double subtotal = 0.0;
        private Double discount = 0.0;
        private Double grossAmount;
        private Double totalAmount = 0.0;
        private Double netPayable;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean isDeleted = false;
        private String deviceId;
        private String publicTokenHash;
        private String publicToken;
        private LocalDateTime tokenCreatedAt;
        private LocalDateTime tokenExpiresAt;
        private LocalDateTime tokenRevokedAt;
        private String clientResponseStatus;
        private LocalDateTime respondedAt;
        private String discussionMessage;
        private Integer version = 1;
        private List<QuotationItem> items = new ArrayList<>();

        public QuotationBuilder id(UUID id) { this.id = id; return this; }
        public QuotationBuilder client(Client client) { this.client = client; return this; }
        public QuotationBuilder user(User user) { this.user = user; return this; }
        public QuotationBuilder quotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; return this; }
        public QuotationBuilder status(QuotationStatus status) { this.status = status; return this; }
        public QuotationBuilder issueDate(LocalDateTime issueDate) { this.issueDate = issueDate; return this; }
        public QuotationBuilder validUntilDate(LocalDateTime validUntilDate) { this.validUntilDate = validUntilDate; return this; }
        public QuotationBuilder notes(String notes) { this.notes = notes; return this; }
        public QuotationBuilder termsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; return this; }
        public QuotationBuilder pdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
        public QuotationBuilder pdfPath(String pdfPath) { this.pdfPath = pdfPath; return this; }
        public QuotationBuilder subtotal(Double subtotal) { this.subtotal = subtotal; return this; }
        public QuotationBuilder discount(Double discount) { this.discount = discount; return this; }
        public QuotationBuilder grossAmount(Double grossAmount) { this.grossAmount = grossAmount; return this; }
        public QuotationBuilder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public QuotationBuilder netPayable(Double netPayable) { this.netPayable = netPayable; return this; }
        public QuotationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public QuotationBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public QuotationBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public QuotationBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public QuotationBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public QuotationBuilder publicTokenHash(String publicTokenHash) { this.publicTokenHash = publicTokenHash; return this; }
        public QuotationBuilder publicToken(String publicToken) { this.publicToken = publicToken; return this; }
        public QuotationBuilder tokenCreatedAt(LocalDateTime tokenCreatedAt) { this.tokenCreatedAt = tokenCreatedAt; return this; }
        public QuotationBuilder tokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; return this; }
        public QuotationBuilder tokenRevokedAt(LocalDateTime tokenRevokedAt) { this.tokenRevokedAt = tokenRevokedAt; return this; }
        public QuotationBuilder clientResponseStatus(String clientResponseStatus) { this.clientResponseStatus = clientResponseStatus; return this; }
        public QuotationBuilder respondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; return this; }
        public QuotationBuilder discussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; return this; }
        public QuotationBuilder version(Integer version) { this.version = version; return this; }
        public QuotationBuilder items(List<QuotationItem> items) { this.items = items; return this; }

        public Quotation build() {
            Quotation q = new Quotation();
            q.id = this.id;
            q.client = this.client;
            q.user = this.user;
            q.quotationNumber = this.quotationNumber;
            q.status = this.status;
            q.issueDate = this.issueDate;
            q.validUntilDate = this.validUntilDate;
            q.notes = this.notes;
            q.termsAndConditions = this.termsAndConditions;
            q.pdfUrl = this.pdfUrl;
            q.pdfPath = this.pdfPath;
            q.subtotal = this.subtotal;
            q.discount = this.discount;
            q.grossAmount = this.grossAmount;
            q.totalAmount = this.totalAmount;
            q.netPayable = this.netPayable;
            q.createdAt = this.createdAt;
            q.updatedAt = this.updatedAt;
            q.deletedAt = this.deletedAt;
            q.isDeleted = this.isDeleted;
            q.deviceId = this.deviceId;
            q.publicTokenHash = this.publicTokenHash;
            q.publicToken = this.publicToken;
            q.tokenCreatedAt = this.tokenCreatedAt;
            q.tokenExpiresAt = this.tokenExpiresAt;
            q.tokenRevokedAt = this.tokenRevokedAt;
            q.clientResponseStatus = this.clientResponseStatus;
            q.respondedAt = this.respondedAt;
            q.discussionMessage = this.discussionMessage;
            q.version = this.version;
            q.items = this.items;
            return q;
        }
    }
}
