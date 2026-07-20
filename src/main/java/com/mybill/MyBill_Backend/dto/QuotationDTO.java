package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.QuotationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationDTO {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private String quotationNumber;
    private QuotationStatus status;
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
    private Integer version;
    private List<QuotationItemDTO> items;
}
