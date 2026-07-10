package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RecurringInvoiceScheduleDTO {
    private UUID id;

    @NotNull(message = "Client is required")
    private UUID clientId;
    private String clientName;

    @NotBlank(message = "Schedule description is required")
    @Size(max = 250, message = "Schedule description must be 250 characters or fewer")
    private String description;

    @NotNull(message = "Schedule amount is required")
    @DecimalMin(value = "0.01", message = "Schedule amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Schedule amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Billing cycle is required")
    private String billingCycle;
    private String cronExpression;
    private String status;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRunDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRunDate;

    private Boolean autoCharge;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
    
    private Integer version;

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isEndDateOnOrAfterStartDate() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
