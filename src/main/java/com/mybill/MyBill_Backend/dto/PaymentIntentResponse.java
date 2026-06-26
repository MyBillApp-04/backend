package com.mybill.MyBill_Backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PaymentIntentResponse {
    private UUID paymentId;
    private String paymentIntentId;
    private String clientSecret;
    private Double amount;
    private String currency;
    private String status;
}
