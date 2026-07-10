package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Expense description is required")
    @Size(max = 250, message = "Expense description must be 250 characters or fewer")
    private String description;

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Expense amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Expense category is required")
    @Size(max = 80, message = "Expense category must be 80 characters or fewer")
    private String category;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @Size(max = 160, message = "Vendor name must be 160 characters or fewer")
    private String vendorName;

    @DecimalMin(value = "0.00", message = "Tax amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Tax amount can have at most 2 decimal places")
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
