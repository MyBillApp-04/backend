package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class InvoiceNumberService {

    private static final String DEFAULT_COMPANY_CODE = "GKE";
    private static final int MAX_SEQUENCE = 9999;
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final InvoiceSettingsRepository invoiceSettingsRepository;

    private final EntityManager entityManager;

    @Transactional
    public InvoiceNumberResult generateNextInvoiceNumber(Long userId, LocalDate invoiceDate) {
        LocalDate effectiveDate = invoiceDate != null ? invoiceDate : LocalDate.now(INDIA);
        FinancialYear financialYear = financialYearFor(effectiveDate);
        int sequence = reserveSequence(userId, financialYear.label());
        if (sequence > MAX_SEQUENCE) {
            throw new IllegalStateException("Invoice sequence limit reached for " + financialYear.label());
        }

        InvoiceSettings settings = invoiceSettingsRepository.findByUserId(userId).orElse(null);
        String companyCode = companyCode(settings != null ? settings.getInvoicePrefix() : null);
        int defaultDueDays = settings != null && settings.getDefaultDueDays() != null
                ? settings.getDefaultDueDays()
                : 7;
        return new InvoiceNumberResult(
                format(companyCode, financialYear.code(), sequence),
                financialYear.label(),
                sequence,
                defaultDueDays,
                settings != null ? settings.getTermsAndConditions() : null,
                settings != null ? settings.getPaymentNote() : null,
                settings != null ? settings.getUpiId() : null
        );
    }

    /** PostgreSQL UPSERT serializes allocations for one financial year per user. */
    private int reserveSequence(Long userId, String financialYear) {
        Object value = entityManager.createNativeQuery("""
                INSERT INTO public.invoice_financial_year_sequence (user_id, financial_year, last_sequence)
                VALUES (:userId, :financialYear, 1)
                ON CONFLICT (user_id, financial_year) DO UPDATE
                    SET last_sequence = public.invoice_financial_year_sequence.last_sequence + 1
                RETURNING last_sequence
                """)
                .setParameter("userId", userId)
                .setParameter("financialYear", financialYear)
                .getSingleResult();
        return ((Number) value).intValue();
    }

    public static FinancialYear financialYearFor(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        int endYear = startYear + 1;
        return new FinancialYear(
                startYear + "-" + endYear,
                String.format("%02d%02d", startYear % 100, endYear % 100)
        );
    }

    private String format(String companyCode, String financialYearCode, int sequence) {
        return companyCode + "-" + financialYearCode + "-" + String.format("%04d", sequence);
    }

    /** Company code is configured in Invoice Settings; sequence remains server-owned. */
    public static String companyCode(String value) {
        if (value == null || value.isBlank()) return DEFAULT_COMPANY_CODE;
        String normalized = value.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{2,5}")) {
            throw new IllegalArgumentException("Company code must contain 2 to 5 letters or numbers");
        }
        return normalized;
    }

    public record InvoiceNumberResult(
            String invoiceNumber,
            String financialYear,
            int sequenceNo,
            int defaultDueDays,
            String termsAndConditions,
            String paymentNote,
            String upiId
    ) {
    }

    public record FinancialYear(String label, String code) {}
}
