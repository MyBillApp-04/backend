package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSyncPayload {

    private String id;
    private String clientId;
    private String quotationId;

    // Customer GST snapshot taken at invoice-creation time.
    private String clientState;
    private String clientGstin;

    private String invoiceNumber;

    // Math & Tracking
    private Double subtotal;
    private Double discount;
    private Double grossAmount;
    private Double advanceApplied;
    private Double netPayable;
    private Double totalAmount;
    private Double paidAmount;
    private Double pendingAmount;

    // GST / Tax snapshot (invoice-level single rate). Ignored as trusted input;
    // the server recomputes these authoritatively via TaxCalculator.
    private Double taxRate;
    private String taxType;
    private Double taxableAmount;
    private Double taxAmount;
    private Double cgstAmount;
    private Double sgstAmount;
    private Double igstAmount;
    private Double total;

    // Status & Modes (Using String to avoid Enum parsing crashes during sync)
    private String paymentStatus;
    private String paymentMode;

    // Dates
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;
    private LocalDateTime paymentDate;

    // Notes & Files
    private String notes;
    private String pdfUrl;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;
}