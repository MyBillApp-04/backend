package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceSettingsService {

    private final InvoiceSettingsRepository repository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public InvoiceSettings getSettings() {
        Long userId = securityUtils.getCurrentUserId();
        return repository.findByUserId(userId).orElseGet(this::createDefaultSettings);
    }

    public InvoiceSettings saveOrUpdateSettings(InvoiceSettings incoming) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();

        return repository.findByUserId(userId).map(existing -> {
            existing.setInvoicePrefix(InvoiceNumberService.companyCode(incoming.getInvoicePrefix()));
            existing.setNextInvoiceNumber(incoming.getNextInvoiceNumber() != null ? incoming.getNextInvoiceNumber() : 1);
            existing.setDefaultDueDays(incoming.getDefaultDueDays() != null ? incoming.getDefaultDueDays() : 7);
            existing.setTermsAndConditions(clean(incoming.getTermsAndConditions()));
            existing.setPaymentNote(clean(incoming.getPaymentNote()));
            existing.setUpiId(clean(incoming.getUpiId()));
            return repository.save(existing);
        }).orElseGet(() -> {
            incoming.setUser(user);
            incoming.setInvoicePrefix(InvoiceNumberService.companyCode(incoming.getInvoicePrefix()));
            if (incoming.getNextInvoiceNumber() == null) incoming.setNextInvoiceNumber(1);
            if (incoming.getDefaultDueDays() == null) incoming.setDefaultDueDays(7);
            return repository.save(incoming);
        });
    }

    private InvoiceSettings createDefaultSettings() {
        User user = securityUtils.getCurrentUser();
        InvoiceSettings defaults = InvoiceSettings.builder()
                .user(user)
                .invoicePrefix("GKE")
                .nextInvoiceNumber(1)
                .defaultDueDays(7)
                .termsAndConditions("Thank you for your business! Please pay within the due date.")
                .paymentNote("Please mention your invoice number during bank transfers.")
                .build();
        try {
            return repository.save(defaults);
        } catch (Exception e) {
            return defaults;
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
