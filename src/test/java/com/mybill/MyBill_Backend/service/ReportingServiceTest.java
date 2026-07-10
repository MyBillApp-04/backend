package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.AdvancedReportDTO;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportingServiceTest {

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query mockQuery;

    @InjectMocks
    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void advancedReportSuccess() {
        // Arrange
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(30);
        LocalDateTime end = now.plusDays(30);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

        // 1. Aging Report Rows
        UUID invoiceId = UUID.randomUUID();
        Object[] agingRow = new Object[] {
            invoiceId, "INV-001", "Client A", Timestamp.valueOf(now.minusDays(10)), 500.0
        };
        List<Object[]> agingList = java.util.Collections.singletonList(agingRow);

        // 2. Forecast Rows
        Object[] forecastRow = new Object[] {
            UUID.randomUUID(), "INV-002", "Client B", Timestamp.valueOf(now.plusDays(15)), 300.0
        };
        List<Object[]> forecastList = java.util.Collections.singletonList(forecastRow);

        // 3. Client Profitability Rows
        Object[] profitRow = new Object[] {
            UUID.randomUUID(), "Client A", 2, 1000.0, 800.0, 200.0
        };
        List<Object[]> profitList = java.util.Collections.singletonList(profitRow);

        // 4. Payment breakdown Rows
        Object[] paymentRow = new Object[] {
            "CASH", 3, 600.0
        };
        List<Object[]> paymentList = java.util.Collections.singletonList(paymentRow);

        // 5. Pipeline Rows
        Object[] pipelineRow = new Object[] {
            "PAID", 5, 2500.0
        };
        List<Object[]> pipelineList = java.util.Collections.singletonList(pipelineRow);

        // Define consecutive returns for getResultList() or getSingleResult()
        // Standard call order:
        // 1. Aging Report List -> getResultList
        // 2. Forecast List -> getResultList
        // 3. Client Profitability List -> getResultList
        // 4. Payment breakdown -> getResultList
        // 5. Invoice Pipeline -> getResultList
        when(mockQuery.getResultList())
                .thenReturn(agingList)        // 1. Aging
                .thenReturn(forecastList)     // 2. Forecast
                .thenReturn(profitList)       // 3. Profitability
                .thenReturn(paymentList)      // 4. Payments
                .thenReturn(pipelineList);    // 5. Pipeline

        // Overall revenue / overall expense -> getSingleResult()
        when(mockQuery.getSingleResult())
                .thenReturn(2500.0) // Overall Revenue
                .thenReturn(500.0);  // Overall Expense

        // Act
        AdvancedReportDTO result = reportingService.advancedReport(null, start, end);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAgingReport()).isNotNull();
        assertThat(result.getAgingReport().getInvoices()).hasSize(1);
        assertThat(result.getAgingReport().getInvoices().get(0).getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(result.getAgingReport().getBuckets().get(0).getTotalAmount()).isEqualTo(500.0);

        assertThat(result.getRevenueForecast()).isNotNull();
        assertThat(result.getRevenueForecast().getTotalProjected()).isEqualTo(300.0);

        assertThat(result.getClientProfitability()).hasSize(1);
        assertThat(result.getClientProfitability().get(0).getClientName()).isEqualTo("Client A");
        assertThat(result.getClientProfitability().get(0).getRevenueContributionPercent()).isEqualTo(100.0);

        assertThat(result.getPaymentMethodBreakdown()).hasSize(1);
        assertThat(result.getPaymentMethodBreakdown().get(0).getPaymentMode()).isEqualTo("CASH");

        // The pipeline stage list should contain PAID (returned from DB) plus the other 4 default stages (0 amount/count)
        assertThat(result.getInvoicePipeline()).hasSize(5);

        assertThat(result.getTotalRevenue()).isEqualTo(2500.0);
        assertThat(result.getTotalExpenses()).isEqualTo(500.0);
        assertThat(result.getNetProfit()).isEqualTo(2000.0);
    }
}
