package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {
    private UUID id;
    private String description;
    private BigDecimal amount;
    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    private String vendorName;
    private BigDecimal taxAmount;
    private String receiptUrl;
    private Boolean isRecurring;
    private String recurringCycle;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    private Integer version;
}
