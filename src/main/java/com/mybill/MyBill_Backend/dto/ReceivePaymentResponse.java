package com.mybill.MyBill_Backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePaymentResponse {
    private Double receivedAmount;
    private Double appliedToInvoices;
    private Double addedToAdvance;
    private ClientFinancialSummaryDTO summary;
}
