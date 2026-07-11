package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.InvoiceSettingsRequest;
import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.InvoiceSettingsRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvoiceSettingsServiceTest {

    private final InvoiceSettingsRepository repository = mock(InvoiceSettingsRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final InvoiceSettingsService service = new InvoiceSettingsService(repository, securityUtils);

    @Test
    void normalizesBrandingFieldsWhenSavingSettings() {
        User user = User.builder().id(42L).email("owner@example.com").build();
        InvoiceSettings existing = InvoiceSettings.builder().user(user).build();
        InvoiceSettingsRequest incoming = new InvoiceSettingsRequest();
        incoming.setInvoicePrefix("ab");
        incoming.setTemplateStyle("invalid");
        incoming.setThemeColor("blue");
        incoming.setFontFamily("papyrus");
        incoming.setShowLogo(null);
        incoming.setTaxIdLabel(" gstin ");
        incoming.setTaxIdValue(" 27ABCDE1234F1Z5 ");

        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        when(securityUtils.getCurrentUser()).thenReturn(user);
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
}
