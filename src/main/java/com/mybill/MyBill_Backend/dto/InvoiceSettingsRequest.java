package com.mybill.MyBill_Backend.dto;

import lombok.Data;

@Data
public class InvoiceSettingsRequest {
    private String invoicePrefix;
    private Integer nextInvoiceNumber;
    private Integer defaultDueDays;
    private String termsAndConditions;
    private String paymentNote;
    private String upiId;
    private String templateStyle;
    private String themeColor;
    private String fontFamily;
    private Boolean showLogo;
    private String taxIdLabel;
    private String taxIdValue;
}
