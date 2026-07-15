package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ReportSummaryDTO;
import com.mybill.MyBill_Backend.dto.AdvancedReportDTO;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final SecurityUtils securityUtils;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public ReportSummaryDTO summary(Integer year) {
        Long userId = securityUtils.getCurrentUserId();
        int targetYear = year != null ? year : LocalDate.now().getYear();

        Map<String, Object> revenue = singleRow("""
                SELECT
                    COALESCE(SUM(total_amount), 0) AS total_revenue,
                    COALESCE(SUM(paid_amount), 0) AS paid_revenue,
                    COALESCE(SUM(remaining_amount), 0) AS remaining_revenue
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM created_date) = :year
                """, userId, targetYear, "totalRevenue", "paidRevenue", "remainingRevenue");

        Map<String, Object> invoices = singleRow("""
                SELECT
                    COUNT(*) AS total_invoices,
                    COUNT(*) FILTER (WHERE payment_status = 'PAID') AS paid_invoices,
                    COUNT(*) FILTER (WHERE payment_status <> 'PAID') AS pending_invoices
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM created_date) = :year
                """, userId, targetYear, "totalInvoices", "paidInvoices", "pendingInvoices");

        Map<String, Object> clients = singleRow("""
                SELECT COUNT(*) AS total_clients
                FROM clients
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                """, userId, targetYear, "totalClients");

        List<Map<String, Object>> trends = trendRows(userId, targetYear);

        return ReportSummaryDTO.builder()
                .revenue(revenue)
                .invoices(invoices)
                .clients(clients)
                .trends(trends)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> revenueTrends(Integer year) {
        Long userId = securityUtils.getCurrentUserId();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return trendRows(userId, targetYear);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics(Integer year) {
        Long userId = securityUtils.getCurrentUserId();
        int targetYear = year != null ? year : LocalDate.now().getYear();

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("year", targetYear);

        Map<String, Object> revMetrics = singleRow("""
                SELECT
                    COALESCE(SUM(total_amount), 0) AS total_revenue,
                    COALESCE(SUM(paid_amount), 0) AS paid_revenue,
                    COALESCE(SUM(remaining_amount), 0) AS outstanding_revenue,
                    COALESCE(AVG(total_amount), 0) AS average_invoice_value
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM created_date) = :year
                """, userId, targetYear, "totalRevenue", "paidRevenue", "outstandingRevenue", "averageInvoiceValue");

        Object totalExpenseVal = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0.0)
                FROM expenses
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM expense_date) = :year
                """)
                .setParameter("userId", userId)
                .setParameter("year", targetYear)
                .getSingleResult();

        double totalRevenue = ((Number) revMetrics.get("totalRevenue")).doubleValue();
        double paidRevenue = ((Number) revMetrics.get("paidRevenue")).doubleValue();
        double expenses = totalExpenseVal != null ? ((Number) totalExpenseVal).doubleValue() : 0.0;
        double netProfit = paidRevenue - expenses;
        double profitMargin = paidRevenue > 0 ? (netProfit / paidRevenue) * 100.0 : 0.0;

        Map<String, Object> pAndL = new LinkedHashMap<>();
        pAndL.put("totalRevenue", totalRevenue);
        pAndL.put("paidRevenue", paidRevenue);
        pAndL.put("outstandingRevenue", revMetrics.get("outstandingRevenue"));
        pAndL.put("averageInvoiceValue", revMetrics.get("averageInvoiceValue"));
        pAndL.put("totalExpenses", expenses);
        pAndL.put("netProfit", netProfit);
        pAndL.put("profitMargin", profitMargin);

        analytics.put("revenue", pAndL);
        analytics.put("paymentStatus", paymentStatusRows(userId, targetYear));
        analytics.put("topClients", topClientRows(userId, targetYear));
        analytics.put("revenueTrends", trendRows(userId, targetYear));
        return analytics;
    }

    private Map<String, Object> singleRow(String sql, Long userId, int year, String... keys) {
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId);

        if (sql.contains(":year")) {
            query.setParameter("year", year);
        }

        Object row = query.getSingleResult();
        Object[] values = row instanceof Object[] ? (Object[]) row : new Object[]{row};

        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            result.put(keys[i], values[i]);
        }
        return result;
    }

    private List<Map<String, Object>> trendRows(Long userId, int year) {
        List<Object[]> revenueRows = entityManager.createNativeQuery("""
                SELECT
                    EXTRACT(MONTH FROM created_date) AS month,
                    COALESCE(SUM(total_amount), 0) AS total_revenue,
                    COALESCE(SUM(paid_amount), 0) AS paid_revenue,
                    COUNT(*) AS invoice_count
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM created_date) = :year
                GROUP BY EXTRACT(MONTH FROM created_date)
                ORDER BY month
                """)
                .setParameter("userId", userId)
                .setParameter("year", year)
                .getResultList();

        List<Object[]> expenseRows = entityManager.createNativeQuery("""
                SELECT
                    EXTRACT(MONTH FROM expense_date) AS month,
                    COALESCE(SUM(amount), 0) AS total_expense
                FROM expenses
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM expense_date) = :year
                GROUP BY EXTRACT(MONTH FROM expense_date)
                ORDER BY month
                """)
                .setParameter("userId", userId)
                .setParameter("year", year)
                .getResultList();

        Map<Integer, Double> monthlyExpenses = new LinkedHashMap<>();
        for (Object[] r : expenseRows) {
            int month = ((Number) r[0]).intValue();
            double totalExpense = ((Number) r[1]).doubleValue();
            monthlyExpenses.put(month, totalExpense);
        }

        Map<Integer, Map<String, Object>> monthlyData = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("month", m);
            data.put("totalRevenue", 0.0);
            data.put("paidRevenue", 0.0);
            data.put("totalExpense", 0.0);
            data.put("invoiceCount", 0);
            monthlyData.put(m, data);
        }

        for (Object[] r : revenueRows) {
            int month = ((Number) r[0]).intValue();
            Map<String, Object> data = monthlyData.get(month);
            if (data != null) {
                data.put("totalRevenue", ((Number) r[1]).doubleValue());
                data.put("paidRevenue", ((Number) r[2]).doubleValue());
                data.put("invoiceCount", ((Number) r[3]).intValue());
            }
        }

        for (Map.Entry<Integer, Double> entry : monthlyExpenses.entrySet()) {
            Map<String, Object> data = monthlyData.get(entry.getKey());
            if (data != null) {
                data.put("totalExpense", entry.getValue());
            }
        }

        return List.copyOf(monthlyData.values());
    }

    private List<Map<String, Object>> paymentStatusRows(Long userId, int year) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    payment_status,
                    COUNT(*) AS invoice_count,
                    COALESCE(SUM(total_amount), 0) AS total_amount,
                    COALESCE(SUM(remaining_amount), 0) AS remaining_amount
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND EXTRACT(YEAR FROM created_date) = :year
                GROUP BY payment_status
                ORDER BY payment_status
                """)
                .setParameter("userId", userId)
                .setParameter("year", year)
                .getResultList();

        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", row[0]);
            m.put("invoiceCount", row[1]);
            m.put("totalAmount", row[2]);
            m.put("remainingAmount", row[3]);
            return m;
        }).toList();
    }

    private List<Map<String, Object>> topClientRows(Long userId, int year) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    c.id,
                    c.name,
                    COUNT(i.id) AS invoice_count,
                    COALESCE(SUM(i.total_amount), 0) AS total_revenue,
                    COALESCE(SUM(i.paid_amount), 0) AS paid_revenue
                FROM invoice i
                JOIN clients c ON c.id = i.client_id
                WHERE i.user_id = :userId
                  AND COALESCE(i.is_deleted, false) = false
                  AND COALESCE(c.is_deleted, false) = false
                  AND EXTRACT(YEAR FROM i.created_date) = :year
                GROUP BY c.id, c.name
                ORDER BY total_revenue DESC
                LIMIT 10
                """)
                .setParameter("userId", userId)
                .setParameter("year", year)
                .getResultList();

        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clientId", row[0]);
            m.put("clientName", row[1]);
            m.put("invoiceCount", row[2]);
            m.put("totalRevenue", row[3]);
            m.put("paidRevenue", row[4]);
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public AdvancedReportDTO advancedReport(Integer year, LocalDateTime startDate, LocalDateTime endDate) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDateTime targetDate = LocalDateTime.now();

        LocalDateTime start = startDate;
        LocalDateTime end = endDate;
        if (start == null && end == null) {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            start = LocalDateTime.of(targetYear, 1, 1, 0, 0, 0);
            end = LocalDateTime.of(targetYear, 12, 31, 23, 59, 59);
        }

        // 1. Aging Report
        List<Object[]> agingRows = entityManager.createNativeQuery("""
                SELECT
                    i.id AS invoice_id,
                    i.invoice_number,
                    c.name AS client_name,
                    i.due_date,
                    i.remaining_amount
                FROM invoice i
                JOIN clients c ON i.client_id = c.id
                WHERE i.user_id = :userId
                  AND COALESCE(i.is_deleted, false) = false
                  AND COALESCE(c.is_deleted, false) = false
                  AND i.payment_status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND i.due_date < :targetDate
                  AND i.invoice_date >= :start
                  AND i.invoice_date <= :end
                ORDER BY i.due_date ASC
                LIMIT 500
                """)
                .setParameter("userId", userId)
                .setParameter("targetDate", targetDate)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        double bucket1Amt = 0; int bucket1Count = 0;
        double bucket2Amt = 0; int bucket2Count = 0;
        double bucket3Amt = 0; int bucket3Count = 0;
        double bucket4Amt = 0; int bucket4Count = 0;

        List<AdvancedReportDTO.AgingReportDetail> agingDetails = new ArrayList<>();
        for (Object[] r : agingRows) {
            UUID invId = (UUID) r[0];
            String invNum = (String) r[1];
            String clientName = (String) r[2];
            LocalDateTime dueDate = ((java.sql.Timestamp) r[3]).toLocalDateTime();
            double remainingAmount = ((Number) r[4]).doubleValue();

            long daysOverdue = ChronoUnit.DAYS.between(dueDate.toLocalDate(), targetDate.toLocalDate());
            if (daysOverdue < 0) daysOverdue = 0;

            AdvancedReportDTO.AgingReportDetail detail = AdvancedReportDTO.AgingReportDetail.builder()
                    .invoiceId(invId)
                    .invoiceNumber(invNum)
                    .clientName(clientName)
                    .dueDate(dueDate)
                    .remainingAmount(remainingAmount)
                    .daysOverdue(daysOverdue)
                    .build();
            agingDetails.add(detail);

            if (daysOverdue <= 30) {
                bucket1Amt += remainingAmount;
                bucket1Count++;
            } else if (daysOverdue <= 60) {
                bucket2Amt += remainingAmount;
                bucket2Count++;
            } else if (daysOverdue <= 90) {
                bucket3Amt += remainingAmount;
                bucket3Count++;
            } else {
                bucket4Amt += remainingAmount;
                bucket4Count++;
            }
        }

        List<AdvancedReportDTO.AgingReportBucket> agingBuckets = List.of(
                new AdvancedReportDTO.AgingReportBucket("1-30 Days Overdue", bucket1Count, bucket1Amt),
                new AdvancedReportDTO.AgingReportBucket("31-60 Days Overdue", bucket2Count, bucket2Amt),
                new AdvancedReportDTO.AgingReportBucket("61-90 Days Overdue", bucket3Count, bucket3Amt),
                new AdvancedReportDTO.AgingReportBucket("90+ Days Overdue", bucket4Count, bucket4Amt)
        );

        AdvancedReportDTO.AgingReport agingReport = AdvancedReportDTO.AgingReport.builder()
                .buckets(agingBuckets)
                .invoices(agingDetails)
                .build();

        // 2. Revenue Forecast
        List<Object[]> forecastRows = entityManager.createNativeQuery("""
                SELECT
                    i.id AS invoice_id,
                    i.invoice_number,
                    c.name AS client_name,
                    i.due_date,
                    i.remaining_amount
                FROM invoice i
                JOIN clients c ON i.client_id = c.id
                WHERE i.user_id = :userId
                  AND COALESCE(i.is_deleted, false) = false
                  AND COALESCE(c.is_deleted, false) = false
                  AND i.payment_status IN ('UNPAID', 'PARTIALLY_PAID')
                  AND i.due_date >= :targetDate
                  AND i.invoice_date >= :start
                  AND i.invoice_date <= :end
                ORDER BY i.due_date ASC
                LIMIT 500
                """)
                .setParameter("userId", userId)
                .setParameter("targetDate", targetDate)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        double fBucket1Amt = 0; int fBucket1Count = 0;
        double fBucket2Amt = 0; int fBucket2Count = 0;
        double fBucket3Amt = 0; int fBucket3Count = 0;
        double fBucket4Amt = 0; int fBucket4Count = 0;
        double totalProjected = 0.0;

        for (Object[] r : forecastRows) {
            LocalDateTime dueDate = ((java.sql.Timestamp) r[3]).toLocalDateTime();
            double remainingAmount = ((Number) r[4]).doubleValue();
            totalProjected += remainingAmount;

            long daysRemaining = ChronoUnit.DAYS.between(targetDate.toLocalDate(), dueDate.toLocalDate());
            if (daysRemaining < 0) daysRemaining = 0;

            if (daysRemaining <= 30) {
                fBucket1Amt += remainingAmount;
                fBucket1Count++;
            } else if (daysRemaining <= 60) {
                fBucket2Amt += remainingAmount;
                fBucket2Count++;
            } else if (daysRemaining <= 90) {
                fBucket3Amt += remainingAmount;
                fBucket3Count++;
            } else {
                fBucket4Amt += remainingAmount;
                fBucket4Count++;
            }
        }

        List<AdvancedReportDTO.RevenueForecastBucket> forecastBuckets = List.of(
                new AdvancedReportDTO.RevenueForecastBucket("0-30 Days Forecast", fBucket1Count, fBucket1Amt),
                new AdvancedReportDTO.RevenueForecastBucket("31-60 Days Forecast", fBucket2Count, fBucket2Amt),
                new AdvancedReportDTO.RevenueForecastBucket("61-90 Days Forecast", fBucket3Count, fBucket3Amt),
                new AdvancedReportDTO.RevenueForecastBucket("90+ Days Forecast", fBucket4Count, fBucket4Amt)
        );

        AdvancedReportDTO.RevenueForecast revenueForecast = AdvancedReportDTO.RevenueForecast.builder()
                .buckets(forecastBuckets)
                .totalProjected(totalProjected)
                .build();

        // 3. Client Profitability
        List<Object[]> clientProfitRows = entityManager.createNativeQuery("""
                SELECT
                    c.id AS client_id,
                    c.name AS client_name,
                    COUNT(i.id) AS invoice_count,
                    COALESCE(SUM(i.total_amount), 0) AS total_billed,
                    COALESCE(SUM(i.paid_amount), 0) AS total_paid,
                    COALESCE(SUM(i.remaining_amount), 0) AS outstanding_amount
                FROM clients c
                LEFT JOIN invoice i ON c.id = i.client_id 
                   AND COALESCE(i.is_deleted, false) = false 
                   AND i.invoice_date >= :start 
                   AND i.invoice_date <= :end
                WHERE c.user_id = :userId
                  AND COALESCE(c.is_deleted, false) = false
                GROUP BY c.id, c.name
                ORDER BY total_billed DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        double overallBilled = 0.0;
        List<AdvancedReportDTO.ClientProfitability> clientProfitabilityList = new ArrayList<>();

        for (Object[] r : clientProfitRows) {
            UUID clientId = (UUID) r[0];
            String clientName = (String) r[1];
            int invoiceCount = ((Number) r[2]).intValue();
            double totalBilled = ((Number) r[3]).doubleValue();
            double totalPaid = ((Number) r[4]).doubleValue();
            double outstandingAmount = ((Number) r[5]).doubleValue();
            double averageInvoiceValue = invoiceCount > 0 ? totalBilled / invoiceCount : 0.0;

            overallBilled += totalBilled;

            AdvancedReportDTO.ClientProfitability item = AdvancedReportDTO.ClientProfitability.builder()
                    .clientId(clientId)
                    .clientName(clientName)
                    .invoiceCount(invoiceCount)
                    .totalBilled(totalBilled)
                    .totalPaid(totalPaid)
                    .outstandingAmount(outstandingAmount)
                    .averageInvoiceValue(averageInvoiceValue)
                    .build();
            clientProfitabilityList.add(item);
        }

        for (AdvancedReportDTO.ClientProfitability item : clientProfitabilityList) {
            double percent = overallBilled > 0 ? (item.getTotalBilled() / overallBilled) * 100.0 : 0.0;
            item.setRevenueContributionPercent(percent);
        }

        // 4. Payment Method Breakdown
        List<Object[]> paymentBreakdownRows = entityManager.createNativeQuery("""
                SELECT
                    p.payment_mode,
                    COUNT(p.payment_id) AS payment_count,
                    COALESCE(SUM(p.amount), 0) AS total_amount
                FROM payments p
                WHERE p.user_id = :userId
                  AND COALESCE(p.is_deleted, false) = false
                  AND p.date >= :start
                  AND p.date <= :end
                GROUP BY p.payment_mode
                ORDER BY total_amount DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        List<AdvancedReportDTO.PaymentMethodBreakdown> paymentBreakdowns = paymentBreakdownRows.stream().map(r -> {
            String mode = r[0] != null ? r[0].toString() : "UNKNOWN";
            int count = ((Number) r[1]).intValue();
            double totalAmount = ((Number) r[2]).doubleValue();
            return new AdvancedReportDTO.PaymentMethodBreakdown(mode, count, totalAmount);
        }).collect(Collectors.toList());

        // 5. Invoice Pipeline Stage
        List<Object[]> pipelineRows = entityManager.createNativeQuery("""
                SELECT
                    CASE
                        WHEN i.payment_status = 'UNPAID' AND NOT EXISTS (
                            SELECT 1 FROM customer_notification_logs log
                            WHERE log.invoice_id = i.id AND log.status = 'SENT'
                        ) THEN 'DRAFT'
                        WHEN i.payment_status = 'UNPAID' AND EXISTS (
                            SELECT 1 FROM customer_notification_logs log
                            WHERE log.invoice_id = i.id AND log.status = 'SENT'
                        ) THEN 'SENT'
                        ELSE CAST(i.payment_status AS VARCHAR)
                    END AS stage,
                    COUNT(*) AS stage_count,
                    COALESCE(SUM(i.total_amount), 0) AS total_amount
                FROM invoice i
                WHERE i.user_id = :userId
                  AND COALESCE(i.is_deleted, false) = false
                  AND i.invoice_date >= :start
                  AND i.invoice_date <= :end
                GROUP BY
                    CASE
                        WHEN i.payment_status = 'UNPAID' AND NOT EXISTS (
                            SELECT 1 FROM customer_notification_logs log
                            WHERE log.invoice_id = i.id AND log.status = 'SENT'
                        ) THEN 'DRAFT'
                        WHEN i.payment_status = 'UNPAID' AND EXISTS (
                            SELECT 1 FROM customer_notification_logs log
                            WHERE log.invoice_id = i.id AND log.status = 'SENT'
                        ) THEN 'SENT'
                        ELSE CAST(i.payment_status AS VARCHAR)
                    END
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        List<AdvancedReportDTO.InvoicePipelineStage> pipelineStages = pipelineRows.stream().map(r -> {
            String stage = r[0] != null ? r[0].toString() : "UNKNOWN";
            int count = ((Number) r[1]).intValue();
            double totalAmount = ((Number) r[2]).doubleValue();
            return new AdvancedReportDTO.InvoicePipelineStage(stage, count, totalAmount);
        }).collect(Collectors.toList());

        List<String> allStages = List.of("DRAFT", "SENT", "PARTIALLY_PAID", "PAID", "CANCELLED");
        for (String stageName : allStages) {
            boolean exists = pipelineStages.stream().anyMatch(ps -> ps.getStage().equalsIgnoreCase(stageName));
            if (!exists) {
                pipelineStages.add(new AdvancedReportDTO.InvoicePipelineStage(stageName, 0, 0.0));
            }
        }

        // 6. Overall Revenue, Expenses, Profit
        Object totalRevenueVal = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(paid_amount), 0.0)
                FROM invoice
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND invoice_date >= :start
                  AND invoice_date <= :end
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        Object totalExpenseVal = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0.0)
                FROM expenses
                WHERE user_id = :userId
                  AND COALESCE(is_deleted, false) = false
                  AND expense_date >= :startDate
                  AND expense_date <= :endDate
                """)
                .setParameter("userId", userId)
                .setParameter("startDate", start.toLocalDate())
                .setParameter("endDate", end.toLocalDate())
                .getSingleResult();

        double totalRevenue = ((Number) totalRevenueVal).doubleValue();
        double totalExpenses = ((Number) totalExpenseVal).doubleValue();
        double netProfit = totalRevenue - totalExpenses;

        return AdvancedReportDTO.builder()
                .agingReport(agingReport)
                .revenueForecast(revenueForecast)
                .clientProfitability(clientProfitabilityList)
                .paymentMethodBreakdown(paymentBreakdowns)
                .invoicePipeline(pipelineStages)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .build();
    }
}
