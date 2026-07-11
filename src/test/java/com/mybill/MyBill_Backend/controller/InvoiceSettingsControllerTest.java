package com.mybill.MyBill_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.InvoiceSettingsRequest;
import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.service.InvoiceSettingsService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceSettingsControllerTest {

    private final InvoiceSettingsService service = mock(InvoiceSettingsService.class);
    private final InvoiceSettingsController controller =
            new InvoiceSettingsController(service, new ObjectMapper());

    @Test
    void acceptsJsonPayloadSentAsTextPlainForOlderApkCompatibility() {
        InvoiceSettings saved = InvoiceSettings.builder()
                .invoicePrefix("INV")
                .themeColor("#225378")
                .build();

        when(service.saveOrUpdateSettings(argThat(request ->
                "inv".equals(request.getInvoicePrefix())
                        && "#225378".equals(request.getThemeColor())
                        && Boolean.TRUE.equals(request.getShowLogo())
        ))).thenReturn(saved);

        var response = controller.updateSettingsFromText("""
                {
                  "invoicePrefix": "inv",
                  "themeColor": "#225378",
                  "showLogo": true
                }
                """);

        assertThat(response.getBody()).isSameAs(saved);
    }

    @Test
    void acceptsApplicationJsonDtoPayload() {
        InvoiceSettingsRequest request = new InvoiceSettingsRequest();
        request.setInvoicePrefix("inv");

        InvoiceSettings saved = InvoiceSettings.builder()
                .invoicePrefix("INV")
                .build();

        when(service.saveOrUpdateSettings(request)).thenReturn(saved);

        var response = controller.updateSettings(request);

        assertThat(response.getBody()).isSameAs(saved);
    }
}
