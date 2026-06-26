package com.mybill.MyBill_Backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ClientSummaryDTO {

    private UUID clientId;
    private String clientName;
    private Long totalWorks;
    private Double totalAmount;

    public ClientSummaryDTO(UUID clientId, String clientName, Long totalWorks, Double totalAmount) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.totalWorks = totalWorks;
        this.totalAmount = totalAmount != null ? totalAmount : 0.0;
    }
}