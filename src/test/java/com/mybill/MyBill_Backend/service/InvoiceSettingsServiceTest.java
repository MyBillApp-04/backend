package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.InvoiceSettingsRequest;
import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceSettingsServiceTest {

    private InvoiceSettingsRepository repository;
    private SecurityUtils securityUtils;
    private InvoiceSettingsService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        repository = mock(InvoiceSettingsRepository.class);
        securityUtils = mock(SecurityUtils.class);
        service = new InvoiceSettingsService(repository, securityUtils);

        testUser = User.builder().id(42L).email("owner@example.com").build();
        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void returnsExistingSettingsWhenFound() {
        InvoiceSettings existing = InvoiceSettings.builder().user(testUser).invoicePrefix("INV").build();
        when(repository.findByUserId(42L)).thenReturn(Optional.of(existing));

        InvoiceSettings result = service.getSettings();

        assertThat(result).isNotNull();
        assertThat(result.getInvoicePrefix()).isEqualTo("INV");
        verify(repository, times(1)).findByUserId(42L);
    }

    @Test
    void createsDefaultSettingsWhenNoneExist() {
        when(repository.findByUserId(42L)).thenReturn(Optional.empty());
        when(repository.save(any(InvoiceSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceSettings defaults = service.getSettings();

        assertThat(defaults).isNotNull();
        assertThat(defaults.getInvoicePrefix()).isEqualTo("GKE");
        assertThat(defaults.getDefaultDueDays()).isEqualTo(7);
        assertThat(defaults.getTemplateStyle()).isEqualTo("CLASSIC");
        assertThat(defaults.getThemeColor()).isEqualTo("#225378");
        assertThat(defaults.getFontFamily()).isEqualTo("HELVETICA");
        assertThat(defaults.getShowLogo()).isTrue();
    }

    @Test
    void normalizesBrandingFieldsWhenSavingSettings() {
        InvoiceSettings existing = InvoiceSettings.builder().user(testUser).build();
        InvoiceSettingsRequest incoming = new InvoiceSettingsRequest();
        incoming.setInvoicePrefix("ab");
        incoming.setTemplateStyle("invalid");
        incoming.setThemeColor("blue");
        incoming.setFontFamily("papyrus");
        incoming.setShowLogo(null);
        incoming.setTaxIdLabel(" gstin ");
        incoming.setTaxIdValue(" 27ABCDE1234F1Z5 ");

        when(repository.findByUserId(42L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        InvoiceSettings saved = service.saveOrUpdateSettings(incoming);

        assertThat(saved.getInvoicePrefix()).isEqualTo("AB");
        assertThat(saved.getTemplateStyle()).isEqualTo("CLASSIC");
        assertThat(saved.getThemeColor()).isEqualTo("#225378");
        assertThat(saved.getFontFamily()).isEqualTo("HELVETICA");
        assertThat(saved.getShowLogo()).isTrue();
        assertThat(saved.getTaxIdLabel()).isEqualTo("gstin");
        assertThat(saved.getTaxIdValue()).isEqualTo("27ABCDE1234F1Z5");
    }

    @Test
    void createsNewSettingsWhenUpdatingIfNoneExist() {
        InvoiceSettingsRequest incoming = new InvoiceSettingsRequest();
        incoming.setInvoicePrefix("MYB");
        incoming.setTemplateStyle("MODERN");
        incoming.setThemeColor("#123456");
        incoming.setFontFamily("TIMES");

        when(repository.findByUserId(42L)).thenReturn(Optional.empty());
        when(repository.save(any(InvoiceSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceSettings saved = service.saveOrUpdateSettings(incoming);

        assertThat(saved).isNotNull();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getInvoicePrefix()).isEqualTo("MYB");
        assertThat(saved.getTemplateStyle()).isEqualTo("MODERN");
        assertThat(saved.getThemeColor()).isEqualTo("#123456");
        assertThat(saved.getFontFamily()).isEqualTo("TIMES");
    }
}
