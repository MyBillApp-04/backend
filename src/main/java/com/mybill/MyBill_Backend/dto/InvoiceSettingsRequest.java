package com.mybill.MyBill_Backend.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class InvoiceSettingsRequest {
    @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]*") private String invoicePrefix;
    @Min(1) @Max(999999999) private Integer nextInvoiceNumber;
    @Min(0) @Max(365) private Integer defaultDueDays;
    @Size(max = 10000) private String termsAndConditions;
    @Size(max = 1000) private String paymentNote;
    @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._@-]*") private String upiId;
    @Size(max = 50) private String templateStyle;
    @Pattern(regexp = "#[0-9A-Fa-f]{6}") private String themeColor;
    @Size(max = 100) private String fontFamily;
    private Boolean showLogo;
    @Size(max = 100) private String taxIdLabel;
    @Size(max = 100) private String taxIdValue;

    @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]*") private String quotationPrefix;
    @Min(1) @Max(999999999) private Integer nextQuotationNumber;
    @Min(0) @Max(3650) private Integer defaultQuotationValidityDays;

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
}
