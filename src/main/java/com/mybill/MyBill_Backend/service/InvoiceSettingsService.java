package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceSettingsService {

    private final InvoiceSettingsRepository repository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @Cacheable(value = "invoiceSettings", key = "@securityUtils.getCurrentUserId()")
    public InvoiceSettings getSettings() {
        Long userId = securityUtils.getCurrentUserId();
        return repository.findByUserId(userId).orElseGet(this::createDefaultSettings);
    }

    @CacheEvict(value = "invoiceSettings", key = "@securityUtils.getCurrentUserId()")
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
            existing.setTemplateStyle(incoming.getTemplateStyle() != null ? incoming.getTemplateStyle() : "CLASSIC");
            existing.setThemeColor(incoming.getThemeColor() != null ? incoming.getThemeColor() : "#225378");
            existing.setFontFamily(incoming.getFontFamily() != null ? incoming.getFontFamily() : "HELVETICA");
            existing.setShowLogo(incoming.getShowLogo() != null ? incoming.getShowLogo() : true);
            existing.setTaxIdLabel(incoming.getTaxIdLabel() != null ? incoming.getTaxIdLabel() : "");
            existing.setTaxIdValue(incoming.getTaxIdValue() != null ? incoming.getTaxIdValue() : "");
            return repository.save(existing);
        }).orElseGet(() -> {
            incoming.setUser(user);
            incoming.setInvoicePrefix(InvoiceNumberService.companyCode(incoming.getInvoicePrefix()));
            if (incoming.getNextInvoiceNumber() == null) incoming.setNextInvoiceNumber(1);
            if (incoming.getDefaultDueDays() == null) incoming.setDefaultDueDays(7);
            if (incoming.getTemplateStyle() == null) incoming.setTemplateStyle("CLASSIC");
            if (incoming.getThemeColor() == null) incoming.setThemeColor("#225378");
            if (incoming.getFontFamily() == null) incoming.setFontFamily("HELVETICA");
            if (incoming.getShowLogo() == null) incoming.setShowLogo(true);
            if (incoming.getTaxIdLabel() == null) incoming.setTaxIdLabel("");
            if (incoming.getTaxIdValue() == null) incoming.setTaxIdValue("");
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
                .templateStyle("CLASSIC")
                .themeColor("#225378")
                .fontFamily("HELVETICA")
                .showLogo(true)
                .taxIdLabel("")
                .taxIdValue("")
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
