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
}
