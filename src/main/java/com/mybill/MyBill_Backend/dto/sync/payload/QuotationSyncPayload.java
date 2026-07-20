package com.mybill.MyBill_Backend.dto.sync.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationSyncPayload {

    private String id;
    private String clientId;

    private String quotationNumber;
    private String status;

    private LocalDateTime issueDate;
    private LocalDateTime validUntilDate;

    private String notes;
    private String termsAndConditions;

    private String pdfUrl;
    private String pdfPath;

    private Double subtotal;
    private Double discount;
    private Double grossAmount;
    private Double totalAmount;
    private Double netPayable;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    private String deviceId;
    private String userKey;
}
