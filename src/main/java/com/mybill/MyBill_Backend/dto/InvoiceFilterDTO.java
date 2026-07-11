package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.PaymentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceFilterDTO {
    @Size(max = 120, message = "Search query must be 120 characters or fewer")
    private String query;
    private UUID clientId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<PaymentStatus> statuses;

    @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative")
    @DecimalMax(value = "999999999999.99", message = "Minimum amount is too large")
    @Digits(integer = 12, fraction = 2, message = "Minimum amount can have at most 2 decimal places")
    private Double minAmount;

    @DecimalMin(value = "0.00", message = "Maximum amount cannot be negative")
    @DecimalMax(value = "999999999999.99", message = "Maximum amount is too large")
    @Digits(integer = 12, fraction = 2, message = "Maximum amount can have at most 2 decimal places")
    private Double maxAmount;

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isEndDateOnOrAfterStartDate() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Maximum amount must be greater than or equal to minimum amount")
    public boolean isMaxAmountGreaterThanOrEqualToMinAmount() {
        return minAmount == null || maxAmount == null || maxAmount >= minAmount;
    }
}
