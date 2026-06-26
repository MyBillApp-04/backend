package com.mybill.MyBill_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSheetResponse {
    private String clientSecret;
    private String customerId;
    private String ephemeralKey;
    private String publishableKey;
    private Double amount;
    private String currency;
}
