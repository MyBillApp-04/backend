package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentVerificationRequest {
    @NotBlank(message = "Payment intent ID is required")
    private String paymentIntentId;
}
