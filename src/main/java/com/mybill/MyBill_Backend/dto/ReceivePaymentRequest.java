package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePaymentRequest {
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @DecimalMax(value = "999999999999.99", message = "Payment amount is too large")
    @Digits(integer = 12, fraction = 2, message = "Payment amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDateTime paymentDate;

    @Size(max = 250, message = "Payment notes must be 250 characters or fewer")
    private String notes;

    @Size(max = 120, message = "Device ID must be 120 characters or fewer")
    private String deviceId;
}
