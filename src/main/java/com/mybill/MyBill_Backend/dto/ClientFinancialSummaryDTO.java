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

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public Double getAdvanceBalance() { return advanceBalance; }
    public void setAdvanceBalance(Double advanceBalance) { this.advanceBalance = advanceBalance; }
    public Double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(Double outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public Double getTotalBilledAmount() { return totalBilledAmount; }
    public void setTotalBilledAmount(Double totalBilledAmount) { this.totalBilledAmount = totalBilledAmount; }
    public Double getTotalReceivedAmount() { return totalReceivedAmount; }
    public void setTotalReceivedAmount(Double totalReceivedAmount) { this.totalReceivedAmount = totalReceivedAmount; }

    public static ClientFinancialSummaryDTOBuilder builder() { return new ClientFinancialSummaryDTOBuilder(); }

    public static class ClientFinancialSummaryDTOBuilder {
        private UUID clientId;
        private Double advanceBalance;
        private Double outstandingBalance;
        private Double totalBilledAmount;
        private Double totalReceivedAmount;

        public ClientFinancialSummaryDTOBuilder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public ClientFinancialSummaryDTOBuilder advanceBalance(Double advanceBalance) { this.advanceBalance = advanceBalance; return this; }
        public ClientFinancialSummaryDTOBuilder outstandingBalance(Double outstandingBalance) { this.outstandingBalance = outstandingBalance; return this; }
        public ClientFinancialSummaryDTOBuilder totalBilledAmount(Double totalBilledAmount) { this.totalBilledAmount = totalBilledAmount; return this; }
        public ClientFinancialSummaryDTOBuilder totalReceivedAmount(Double totalReceivedAmount) { this.totalReceivedAmount = totalReceivedAmount; return this; }

        public ClientFinancialSummaryDTO build() {
            ClientFinancialSummaryDTO dto = new ClientFinancialSummaryDTO();
            dto.clientId = this.clientId;
            dto.advanceBalance = this.advanceBalance;
            dto.outstandingBalance = this.outstandingBalance;
            dto.totalBilledAmount = this.totalBilledAmount;
            dto.totalReceivedAmount = this.totalReceivedAmount;
            return dto;
        }
    }
}
