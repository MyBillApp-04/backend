package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.InvoiceSettingsRequest;
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
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceSettingsService {
    private static final Set<String> TEMPLATE_STYLES = Set.of("CLASSIC", "MODERN", "MINIMAL");
    private static final Set<String> FONT_FAMILIES = Set.of("HELVETICA", "TIMES", "COURIER");
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final InvoiceSettingsRepository repository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @Cacheable(value = "invoiceSettings", key = "@securityUtils.getCurrentUserId()")
    public InvoiceSettings getSettings() {
        Long userId = securityUtils.getCurrentUserId();
        return repository.findByUserId(userId).orElseGet(this::createDefaultSettings);
    }

    @CacheEvict(value = "invoiceSettings", key = "@securityUtils.getCurrentUserId()")
    public InvoiceSettings saveOrUpdateSettings(InvoiceSettingsRequest incoming) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();

        return repository.findByUserId(userId).map(existing -> {
            applyRequest(existing, incoming);
            return repository.save(existing);
        }).orElseGet(() -> {
            InvoiceSettings settings = new InvoiceSettings();
            settings.setUser(user);
            applyRequest(settings, incoming);
            return repository.save(settings);
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

    private String cleanOrEmpty(String value) {
        String cleaned = clean(value);
        return cleaned == null ? "" : cleaned;
    }

    private String normalizeOption(String value, Set<String> allowed, String fallback) {
        String cleaned = clean(value);
        if (cleaned == null) return fallback;
        String normalized = cleaned.toUpperCase();
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String normalizeColor(String value) {
        String cleaned = clean(value);
        if (cleaned == null) return "#225378";
        return HEX_COLOR.matcher(cleaned).matches() ? cleaned.toUpperCase() : "#225378";
    }

    private void applyRequest(InvoiceSettings settings, InvoiceSettingsRequest incoming) {
        settings.setInvoicePrefix(InvoiceNumberService.companyCode(incoming.getInvoicePrefix()));
        settings.setNextInvoiceNumber(incoming.getNextInvoiceNumber() != null ? incoming.getNextInvoiceNumber() : 1);
        settings.setDefaultDueDays(incoming.getDefaultDueDays() != null ? incoming.getDefaultDueDays() : 7);
        settings.setTermsAndConditions(clean(incoming.getTermsAndConditions()));
        settings.setPaymentNote(clean(incoming.getPaymentNote()));
        settings.setUpiId(clean(incoming.getUpiId()));
        settings.setTemplateStyle(normalizeOption(incoming.getTemplateStyle(), TEMPLATE_STYLES, "CLASSIC"));
        settings.setThemeColor(normalizeColor(incoming.getThemeColor()));
        settings.setFontFamily(normalizeOption(incoming.getFontFamily(), FONT_FAMILIES, "HELVETICA"));
        settings.setShowLogo(incoming.getShowLogo() != null ? incoming.getShowLogo() : true);
        settings.setTaxIdLabel(cleanOrEmpty(incoming.getTaxIdLabel()));
        settings.setTaxIdValue(cleanOrEmpty(incoming.getTaxIdValue()));
    }
}
