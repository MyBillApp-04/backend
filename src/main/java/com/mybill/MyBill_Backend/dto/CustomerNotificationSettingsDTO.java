package com.mybill.MyBill_Backend.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNotificationSettingsDTO {
    private UUID id;
    private Boolean enableWhatsApp;
    private Boolean enableInvoiceGenerated;
    private Boolean enablePaymentReceived;
    private Boolean enablePartialPayment;
    private Boolean enableInvoiceUpdated;
    private Boolean enableAdvanceBalance;
    private Boolean enablePaymentReminder;
    private Integer reminderFrequencyDays;
    private Boolean enablePoweredByMyBill;
}
