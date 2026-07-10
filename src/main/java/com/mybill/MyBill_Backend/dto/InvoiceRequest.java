package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {
    @NotNull(message = "Client is required")
    private UUID clientId;

    @NotEmpty(message = "Select at least one work item")
    private List<UUID> workIds;

    @PastOrPresent(message = "Invoice date cannot be in the future")
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "999999999999.99", message = "Discount is too large")
    @Digits(integer = 12, fraction = 2, message = "Discount can have at most 2 decimal places")
    private Double discount;

    @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
    private String notes;

    @AssertTrue(message = "Due date must be on or after invoice date")
    public boolean isDueDateOnOrAfterInvoiceDate() {
        return invoiceDate == null || dueDate == null || !dueDate.isBefore(invoiceDate);
    }
}
