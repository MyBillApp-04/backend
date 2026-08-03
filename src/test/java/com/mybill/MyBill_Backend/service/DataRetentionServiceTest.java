package com.mybill.MyBill_Backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DataRetentionServiceTest {

    private EntityManager entityManager;
    private Query query;
    private DataRetentionService service;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        query = mock(Query.class);
        service = new DataRetentionService(entityManager);

        ReflectionTestUtils.setField(service, "softDeleteRetentionDays", 30);
        ReflectionTestUtils.setField(service, "logRetentionDays", 180);

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("cutoff"), any(LocalDateTime.class))).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
    }

    @Test
    void runRetentionPolicy_ExecutesAllDeletions() {
        service.runRetentionPolicy();

        // Check that query generation is triggered for:
        // 1. FraudCheck associated with old payments
        // 2. ClientLedgerEntry
        // 3. InvoiceItem
        // 4. Payment
        // 5. Invoice
        // 6. ClientWork
        // 7. Client
        // 8. ActivityLog
        // 9. EntityChangeHistory
        verify(entityManager, times(9)).createQuery(anyString());
        verify(query, times(9)).setParameter(eq("cutoff"), any(LocalDateTime.class));
        verify(query, times(9)).executeUpdate();
    }
}
