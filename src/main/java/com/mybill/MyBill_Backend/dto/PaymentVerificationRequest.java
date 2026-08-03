package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentVerificationRequest {
    @NotBlank(message = "Payment intent ID is required")
    @Size(max = 255, message = "Payment intent ID must be 255 characters or fewer")
    @Pattern(regexp = "^pi_[A-Za-z0-9_]+$", message = "Payment intent ID format is invalid")
    private String paymentIntentId;
}
