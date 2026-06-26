package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRequest {
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    private Double amount;

    @Size(max = 250, message = "Reason must be 250 characters or fewer")
    private String reason;
}
