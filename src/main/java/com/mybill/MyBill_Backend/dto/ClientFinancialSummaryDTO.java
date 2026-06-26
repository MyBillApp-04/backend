package com.mybill.MyBill_Backend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientFinancialSummaryDTO {
    private UUID clientId;
    private Double advanceBalance;
    private Double outstandingBalance;
    private Double totalBilledAmount;
    private Double totalReceivedAmount;
}
