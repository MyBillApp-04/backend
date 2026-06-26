package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.PaymentMode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePaymentRequest {
    private Double amount;
    private PaymentMode paymentMode;
    private LocalDateTime paymentDate;
    private String notes;
    private String deviceId;
}
