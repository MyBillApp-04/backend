package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ReportSummaryDTO;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final SecurityUtils securityUtils;

    @PersistenceContext
    private EntityManager entityManager;

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
}
