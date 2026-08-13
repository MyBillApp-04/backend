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

    public Double getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(Double receivedAmount) { this.receivedAmount = receivedAmount; }
    public Double getAppliedToInvoices() { return appliedToInvoices; }
    public void setAppliedToInvoices(Double appliedToInvoices) { this.appliedToInvoices = appliedToInvoices; }
    public Double getAddedToAdvance() { return addedToAdvance; }
    public void setAddedToAdvance(Double addedToAdvance) { this.addedToAdvance = addedToAdvance; }
    public ClientFinancialSummaryDTO getSummary() { return summary; }
    public void setSummary(ClientFinancialSummaryDTO summary) { this.summary = summary; }

    public static ReceivePaymentResponseBuilder builder() { return new ReceivePaymentResponseBuilder(); }

    public static class ReceivePaymentResponseBuilder {
        private Double receivedAmount;
        private Double appliedToInvoices;
        private Double addedToAdvance;
        private ClientFinancialSummaryDTO summary;

        public ReceivePaymentResponseBuilder receivedAmount(Double receivedAmount) { this.receivedAmount = receivedAmount; return this; }
        public ReceivePaymentResponseBuilder appliedToInvoices(Double appliedToInvoices) { this.appliedToInvoices = appliedToInvoices; return this; }
        public ReceivePaymentResponseBuilder addedToAdvance(Double addedToAdvance) { this.addedToAdvance = addedToAdvance; return this; }
        public ReceivePaymentResponseBuilder summary(ClientFinancialSummaryDTO summary) { this.summary = summary; return this; }

        public ReceivePaymentResponse build() {
            ReceivePaymentResponse r = new ReceivePaymentResponse();
            r.receivedAmount = this.receivedAmount;
            r.appliedToInvoices = this.appliedToInvoices;
            r.addedToAdvance = this.addedToAdvance;
            r.summary = this.summary;
            return r;
        }
    }
}
