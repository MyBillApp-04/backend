package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InvoiceNumberServiceTest {

    @Test
    void usesIndianFinancialYearBoundaries() {
        assertThat(InvoiceNumberService.financialYearFor(LocalDate.of(2026, 2, 15)))
                .isEqualTo(new InvoiceNumberService.FinancialYear("2025-2026", "2526"));
        assertThat(InvoiceNumberService.financialYearFor(LocalDate.of(2026, 4, 1)))
                .isEqualTo(new InvoiceNumberService.FinancialYear("2026-2027", "2627"));
        assertThat(InvoiceNumberService.financialYearFor(LocalDate.of(2027, 3, 31)))
                .isEqualTo(new InvoiceNumberService.FinancialYear("2026-2027", "2627"));
        assertThat(InvoiceNumberService.financialYearFor(LocalDate.of(2027, 4, 1)))
                .isEqualTo(new InvoiceNumberService.FinancialYear("2027-2028", "2728"));
    }

    @Test
    void formatsSequencesFromTheAtomicDatabaseAllocator() {
        InvoiceSettingsRepository settingsRepository = mock(InvoiceSettingsRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        InvoiceSettings settings = InvoiceSettings.builder().defaultDueDays(14).build();

        when(settingsRepository.findByUserId(7L)).thenReturn(Optional.of(settings));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1, 2, 3);

        InvoiceNumberService service = new InvoiceNumberService(settingsRepository, entityManager);

        assertThat(service.generateNextInvoiceNumber(7L, LocalDate.of(2026, 6, 24)).invoiceNumber())
                .isEqualTo("GKE-2627-0001");
        assertThat(service.generateNextInvoiceNumber(7L, LocalDate.of(2026, 6, 24)).invoiceNumber())
                .isEqualTo("GKE-2627-0002");
        assertThat(service.generateNextInvoiceNumber(7L, LocalDate.of(2026, 6, 24)).invoiceNumber())
                .isEqualTo("GKE-2627-0003");
    }

    @Test
    void usesTheConfiguredCompanyCode() {
        assertThat(InvoiceNumberService.companyCode("acme1")).isEqualTo("ACME1");
        assertThat(InvoiceNumberService.companyCode(null)).isEqualTo("GKE");
    }
}
