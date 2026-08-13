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
    @JsonIgnore
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

    @Column(name = "quotation_prefix", length = 20)
    @Builder.Default
    private String quotationPrefix = "QT";

    @Column(name = "next_quotation_number")
    @Builder.Default
    private Integer nextQuotationNumber = 1;

    @Column(name = "default_quotation_validity_days")
    @Builder.Default
    private Integer defaultQuotationValidityDays = 30;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }

    public Integer getNextInvoiceNumber() { return nextInvoiceNumber; }
    public void setNextInvoiceNumber(Integer nextInvoiceNumber) { this.nextInvoiceNumber = nextInvoiceNumber; }

    public Integer getDefaultDueDays() { return defaultDueDays; }
    public void setDefaultDueDays(Integer defaultDueDays) { this.defaultDueDays = defaultDueDays; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getPaymentNote() { return paymentNote; }
    public void setPaymentNote(String paymentNote) { this.paymentNote = paymentNote; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getTemplateStyle() { return templateStyle; }
    public void setTemplateStyle(String templateStyle) { this.templateStyle = templateStyle; }

    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public Boolean getShowLogo() { return showLogo; }
    public void setShowLogo(Boolean showLogo) { this.showLogo = showLogo; }

    public String getTaxIdLabel() { return taxIdLabel; }
    public void setTaxIdLabel(String taxIdLabel) { this.taxIdLabel = taxIdLabel; }

    public String getTaxIdValue() { return taxIdValue; }
    public void setTaxIdValue(String taxIdValue) { this.taxIdValue = taxIdValue; }

    public String getQuotationPrefix() { return quotationPrefix; }
    public void setQuotationPrefix(String quotationPrefix) { this.quotationPrefix = quotationPrefix; }

    public Integer getNextQuotationNumber() { return nextQuotationNumber; }
    public void setNextQuotationNumber(Integer nextQuotationNumber) { this.nextQuotationNumber = nextQuotationNumber; }

    public Integer getDefaultQuotationValidityDays() { return defaultQuotationValidityDays; }
    public void setDefaultQuotationValidityDays(Integer defaultQuotationValidityDays) { this.defaultQuotationValidityDays = defaultQuotationValidityDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public static InvoiceSettingsBuilder builder() { return new InvoiceSettingsBuilder(); }

    public static class InvoiceSettingsBuilder {
        private UUID id;
        private User user;
        private String invoicePrefix;
        private Integer nextInvoiceNumber = 1;
        private Integer defaultDueDays = 15;
        private String termsAndConditions;
        private String paymentNote;
        private String upiId;
        private String templateStyle = "MODERN";
        private String themeColor = "#2563EB";
        private String fontFamily = "Inter";
        private Boolean showLogo = true;
        private String taxIdLabel = "GSTIN";
        private String taxIdValue;
        private String quotationPrefix = "QT";
        private Integer nextQuotationNumber = 1;
        private Integer defaultQuotationValidityDays = 30;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long createdBy;
        private Long updatedBy;

        public InvoiceSettingsBuilder id(UUID id) { this.id = id; return this; }
        public InvoiceSettingsBuilder user(User user) { this.user = user; return this; }
        public InvoiceSettingsBuilder invoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; return this; }
        public InvoiceSettingsBuilder nextInvoiceNumber(Integer nextInvoiceNumber) { this.nextInvoiceNumber = nextInvoiceNumber; return this; }
        public InvoiceSettingsBuilder defaultDueDays(Integer defaultDueDays) { this.defaultDueDays = defaultDueDays; return this; }
        public InvoiceSettingsBuilder termsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; return this; }
        public InvoiceSettingsBuilder paymentNote(String paymentNote) { this.paymentNote = paymentNote; return this; }
        public InvoiceSettingsBuilder upiId(String upiId) { this.upiId = upiId; return this; }
        public InvoiceSettingsBuilder templateStyle(String templateStyle) { this.templateStyle = templateStyle; return this; }
        public InvoiceSettingsBuilder themeColor(String themeColor) { this.themeColor = themeColor; return this; }
        public InvoiceSettingsBuilder fontFamily(String fontFamily) { this.fontFamily = fontFamily; return this; }
        public InvoiceSettingsBuilder showLogo(Boolean showLogo) { this.showLogo = showLogo; return this; }
        public InvoiceSettingsBuilder taxIdLabel(String taxIdLabel) { this.taxIdLabel = taxIdLabel; return this; }
        public InvoiceSettingsBuilder taxIdValue(String taxIdValue) { this.taxIdValue = taxIdValue; return this; }
        public InvoiceSettingsBuilder quotationPrefix(String quotationPrefix) { this.quotationPrefix = quotationPrefix; return this; }
        public InvoiceSettingsBuilder nextQuotationNumber(Integer nextQuotationNumber) { this.nextQuotationNumber = nextQuotationNumber; return this; }
        public InvoiceSettingsBuilder defaultQuotationValidityDays(Integer defaultQuotationValidityDays) { this.defaultQuotationValidityDays = defaultQuotationValidityDays; return this; }
        public InvoiceSettingsBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public InvoiceSettingsBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public InvoiceSettingsBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public InvoiceSettingsBuilder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }

        public InvoiceSettings build() {
            InvoiceSettings s = new InvoiceSettings();
            s.id = this.id;
            s.user = this.user;
            s.invoicePrefix = this.invoicePrefix;
            s.nextInvoiceNumber = this.nextInvoiceNumber;
            s.defaultDueDays = this.defaultDueDays;
            s.termsAndConditions = this.termsAndConditions;
            s.paymentNote = this.paymentNote;
            s.upiId = this.upiId;
            s.templateStyle = this.templateStyle;
            s.themeColor = this.themeColor;
            s.fontFamily = this.fontFamily;
            s.showLogo = this.showLogo;
            s.taxIdLabel = this.taxIdLabel;
            s.taxIdValue = this.taxIdValue;
            s.quotationPrefix = this.quotationPrefix;
            s.nextQuotationNumber = this.nextQuotationNumber;
            s.defaultQuotationValidityDays = this.defaultQuotationValidityDays;
            s.createdAt = this.createdAt;
            s.updatedAt = this.updatedAt;
            s.createdBy = this.createdBy;
            s.updatedBy = this.updatedBy;
            return s;
        }
    }
}
