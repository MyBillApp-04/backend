package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_invoice_user_number",
                        columnNames = {"user_id", "invoice_number"}
                ),
                @UniqueConstraint(
                        name = "uq_invoice_user_quotation",
                        columnNames = {"user_id", "quotation_id"}
                )
        },
        indexes = {
                @Index(name = "idx_invoice_user_updated", columnList = "user_id, updated_at"),
                @Index(name = "idx_invoice_user_deleted", columnList = "user_id, is_deleted"),
                @Index(name = "idx_invoice_client", columnList = "client_id"),
                @Index(name = "idx_invoice_quotation", columnList = "quotation_id")
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
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

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
    @Column(columnDefinition = "double precision")
    private Double subtotal;
    @Column(columnDefinition = "double precision")
    private Double discount;
    @Column(columnDefinition = "double precision")
    private Double grossAmount;
    @Column(columnDefinition = "double precision")
    private Double advanceApplied;
    @Column(columnDefinition = "double precision")
    private Double netPayable;
    @Column(columnDefinition = "double precision")
    private Double totalAmount;

    // GST / Tax snapshot (invoice-level single rate)
    @Column(name = "tax_rate", columnDefinition = "double precision")
    private Double taxRate;
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", length = 20)
    private TaxType taxType;
    @Column(name = "taxable_amount", columnDefinition = "double precision")
    private Double taxableAmount;
    @Column(name = "tax_amount", columnDefinition = "double precision")
    private Double taxAmount;
    @Column(name = "cgst_amount", columnDefinition = "double precision")
    private Double cgstAmount;
    @Column(name = "sgst_amount", columnDefinition = "double precision")
    private Double sgstAmount;
    @Column(name = "igst_amount", columnDefinition = "double precision")
    private Double igstAmount;
    /** GST-inclusive total BEFORE advance is applied. */
    @Column(name = "total", columnDefinition = "double precision")
    private Double total;

    // Customer GST snapshot taken at invoice-creation time so a tax invoice
    // stays compliant (buyer GSTIN + state) even if the customer is edited later.
    @Column(name = "client_state", length = 100)
    private String clientState;
    @Column(name = "client_gstin", length = 50)
    private String clientGstin;

    // Payment Tracking
    @Column(columnDefinition = "double precision")
    private Double paidAmount;
    @Column(columnDefinition = "double precision")
    private Double pendingAmount;
    @Column(columnDefinition = "double precision")
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

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @JsonIgnore
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
        if (this.taxRate == null) this.taxRate = 0.0;
        if (this.taxType == null) this.taxType = TaxType.NONE;
        if (this.taxableAmount == null) this.taxableAmount = this.grossAmount;
        if (this.taxAmount == null) this.taxAmount = 0.0;
        if (this.cgstAmount == null) this.cgstAmount = 0.0;
        if (this.sgstAmount == null) this.sgstAmount = 0.0;
        if (this.igstAmount == null) this.igstAmount = 0.0;
        if (this.total == null) this.total = this.grossAmount;
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

        // Single source of truth for payment-state derivation. Both
        // applyPaymentUpdate and this lifecycle hook delegate here so pending,
        // remaining, and status never drift apart.
        applyPendingAndStatus();
    }

    /**
     * Derives pending/remaining amount and payment status from the durable
     * totalAmount and paidAmount in one canonical place. Safe to call on every
     * update: recomputing from durable totals never corrupts explicitly stored
     * payment records.
     */
    public void applyPendingAndStatus() {
        if (this.totalAmount == null || this.paidAmount == null) {
            return;
        }
        double pending = Math.max(this.totalAmount - this.paidAmount, 0.0);
        this.pendingAmount = pending;
        this.remainingAmount = pending;

        if (this.totalAmount <= 0 || this.paidAmount >= this.totalAmount) {
            this.paymentStatus = PaymentStatus.PAID;
        } else if (this.paidAmount > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        } else {
            this.paymentStatus = PaymentStatus.UNPAID;
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
    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(Double grossAmount) { this.grossAmount = grossAmount; }
    public Double getAdvanceApplied() { return advanceApplied; }
    public void setAdvanceApplied(Double advanceApplied) { this.advanceApplied = advanceApplied; }
    public Double getNetPayable() { return netPayable; }
    public void setNetPayable(Double netPayable) { this.netPayable = netPayable; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }
    public Double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(Double pendingAmount) { this.pendingAmount = pendingAmount; }
    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getClientState() { return clientState; }
    public void setClientState(String clientState) { this.clientState = clientState; }
    public String getClientGstin() { return clientGstin; }
    public void setClientGstin(String clientGstin) { this.clientGstin = clientGstin; }
    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public static InvoiceBuilder builder() { return new InvoiceBuilder(); }

    public static class InvoiceBuilder {
        private UUID id;
        private Client client;
        private User user;
        private Quotation quotation;
        private String invoiceNumber;
        private String financialYear;
        private Integer sequenceNo;
        private Double subtotal;
        private Double discount;
        private Double grossAmount;
        private Double advanceApplied;
        private Double netPayable;
        private Double totalAmount;
        private Double taxRate = 0.0;
        private TaxType taxType = TaxType.NONE;
        private Double taxableAmount;
        private Double taxAmount = 0.0;
        private Double cgstAmount = 0.0;
        private Double sgstAmount = 0.0;
        private Double igstAmount = 0.0;
        private String clientState;
        private String clientGstin;
        private Double total;
        private Double paidAmount;
        private Double pendingAmount;
        private Double remainingAmount;
        private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
        private PaymentMode paymentMode;
        private LocalDateTime invoiceDate;
        private LocalDateTime dueDate;
        private LocalDateTime paymentDate;
        private LocalDateTime createdDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private String notes;
        private String pdfUrl;
        private String pdfPath;
        private Boolean isDeleted = false;
        private String deviceId;
        private Integer version = 1;

        public InvoiceBuilder id(UUID id) { this.id = id; return this; }
        public InvoiceBuilder client(Client client) { this.client = client; return this; }
        public InvoiceBuilder user(User user) { this.user = user; return this; }
        public InvoiceBuilder quotation(Quotation quotation) { this.quotation = quotation; return this; }
        public InvoiceBuilder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public InvoiceBuilder financialYear(String financialYear) { this.financialYear = financialYear; return this; }
        public InvoiceBuilder sequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; return this; }
        public InvoiceBuilder subtotal(Double subtotal) { this.subtotal = subtotal; return this; }
        public InvoiceBuilder discount(Double discount) { this.discount = discount; return this; }
        public InvoiceBuilder grossAmount(Double grossAmount) { this.grossAmount = grossAmount; return this; }
        public InvoiceBuilder advanceApplied(Double advanceApplied) { this.advanceApplied = advanceApplied; return this; }
        public InvoiceBuilder netPayable(Double netPayable) { this.netPayable = netPayable; return this; }
        public InvoiceBuilder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public InvoiceBuilder taxRate(Double taxRate) { this.taxRate = taxRate; return this; }
        public InvoiceBuilder taxType(TaxType taxType) { this.taxType = taxType; return this; }
        public InvoiceBuilder taxableAmount(Double taxableAmount) { this.taxableAmount = taxableAmount; return this; }
        public InvoiceBuilder taxAmount(Double taxAmount) { this.taxAmount = taxAmount; return this; }
        public InvoiceBuilder cgstAmount(Double cgstAmount) { this.cgstAmount = cgstAmount; return this; }
        public InvoiceBuilder sgstAmount(Double sgstAmount) { this.sgstAmount = sgstAmount; return this; }
        public InvoiceBuilder igstAmount(Double igstAmount) { this.igstAmount = igstAmount; return this; }
        public InvoiceBuilder clientState(String clientState) { this.clientState = clientState; return this; }
        public InvoiceBuilder clientGstin(String clientGstin) { this.clientGstin = clientGstin; return this; }
        public InvoiceBuilder total(Double total) { this.total = total; return this; }
        public InvoiceBuilder paidAmount(Double paidAmount) { this.paidAmount = paidAmount; return this; }
        public InvoiceBuilder pendingAmount(Double pendingAmount) { this.pendingAmount = pendingAmount; return this; }
        public InvoiceBuilder remainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; return this; }
        public InvoiceBuilder paymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public InvoiceBuilder paymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; return this; }
        public InvoiceBuilder invoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; return this; }
        public InvoiceBuilder dueDate(LocalDateTime dueDate) { this.dueDate = dueDate; return this; }
        public InvoiceBuilder paymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; return this; }
        public InvoiceBuilder createdDate(LocalDateTime createdDate) { this.createdDate = createdDate; return this; }
        public InvoiceBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public InvoiceBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public InvoiceBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public InvoiceBuilder notes(String notes) { this.notes = notes; return this; }
        public InvoiceBuilder pdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
        public InvoiceBuilder pdfPath(String pdfPath) { this.pdfPath = pdfPath; return this; }
        public InvoiceBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public InvoiceBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public InvoiceBuilder version(Integer version) { this.version = version; return this; }

        public Invoice build() {
            Invoice inv = new Invoice();
            inv.id = this.id;
            inv.client = this.client;
            inv.user = this.user;
            inv.quotation = this.quotation;
            inv.invoiceNumber = this.invoiceNumber;
            inv.financialYear = this.financialYear;
            inv.sequenceNo = this.sequenceNo;
            inv.subtotal = this.subtotal;
            inv.discount = this.discount;
            inv.grossAmount = this.grossAmount;
            inv.advanceApplied = this.advanceApplied;
            inv.netPayable = this.netPayable;
            inv.totalAmount = this.totalAmount;
            inv.taxRate = this.taxRate;
            inv.taxType = this.taxType;
            inv.taxableAmount = this.taxableAmount;
            inv.taxAmount = this.taxAmount;
            inv.cgstAmount = this.cgstAmount;
            inv.sgstAmount = this.sgstAmount;
            inv.igstAmount = this.igstAmount;
            inv.clientState = this.clientState;
            inv.clientGstin = this.clientGstin;
            inv.total = this.total;
            inv.paidAmount = this.paidAmount;
            inv.pendingAmount = this.pendingAmount;
            inv.remainingAmount = this.remainingAmount;
            inv.paymentStatus = this.paymentStatus;
            inv.paymentMode = this.paymentMode;
            inv.invoiceDate = this.invoiceDate;
            inv.dueDate = this.dueDate;
            inv.paymentDate = this.paymentDate;
            inv.createdDate = this.createdDate;
            inv.updatedAt = this.updatedAt;
            inv.deletedAt = this.deletedAt;
            inv.notes = this.notes;
            inv.pdfUrl = this.pdfUrl;
            inv.pdfPath = this.pdfPath;
            inv.isDeleted = this.isDeleted;
            inv.deviceId = this.deviceId;
            inv.version = this.version;
            return inv;
        }
    }
}
