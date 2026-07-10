package com.mybill.MyBill_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedReportDTO {
    private AgingReport agingReport;
    private RevenueForecast revenueForecast;
    private List<ClientProfitability> clientProfitability;
    private List<PaymentMethodBreakdown> paymentMethodBreakdown;
    private List<InvoicePipelineStage> invoicePipeline;
    private double totalRevenue;
    private double totalExpenses;
    private double netProfit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingReport {
        private List<AgingReportBucket> buckets;
        private List<AgingReportDetail> invoices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingReportBucket {
        private String bucketName;
        private int count;
        private double totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingReportDetail {
        private UUID invoiceId;
        private String invoiceNumber;
        private String clientName;
        private LocalDateTime dueDate;
        private double remainingAmount;
        private long daysOverdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueForecast {
        private List<RevenueForecastBucket> buckets;
        private double totalProjected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueForecastBucket {
        private String bucketName;
        private int count;
        private double totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientProfitability {
        private UUID clientId;
        private String clientName;
        private int invoiceCount;
        private double totalBilled;
        private double totalPaid;
        private double outstandingAmount;
        private double averageInvoiceValue;
        private double revenueContributionPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodBreakdown {
        private String paymentMode;
        private int count;
        private double totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoicePipelineStage {
        private String stage;
        private int count;
        private double totalAmount;
    }
}
