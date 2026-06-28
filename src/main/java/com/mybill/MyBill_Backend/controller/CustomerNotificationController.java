package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.CustomerNotificationSettingsDTO;
import com.mybill.MyBill_Backend.dto.CustomerNotificationTemplateDTO;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.service.notification.CustomerNotificationService;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/customer-notifications")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final CustomerNotificationService notificationService;
    private final CustomerNotificationSettingsRepository settingsRepository;
    private final CustomerNotificationTemplateRepository templateRepository;
    private final CustomerNotificationLogRepository logRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/settings")
    public ResponseEntity<CustomerNotificationSettingsDTO> getSettings() {
        User user = securityUtils.getCurrentUser();
        CustomerNotificationSettings settings = notificationService.getOrInitializeSettings(user);
        return ResponseEntity.ok(toSettingsDTO(settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<CustomerNotificationSettingsDTO> updateSettings(
            @RequestBody CustomerNotificationSettingsDTO dto
    ) {
        User user = securityUtils.getCurrentUser();
        CustomerNotificationSettings settings = notificationService.getOrInitializeSettings(user);

        if (dto.getEnableWhatsApp() != null) settings.setEnableWhatsApp(dto.getEnableWhatsApp());
        if (dto.getEnableInvoiceGenerated() != null) settings.setEnableInvoiceGenerated(dto.getEnableInvoiceGenerated());
        if (dto.getEnablePaymentReceived() != null) settings.setEnablePaymentReceived(dto.getEnablePaymentReceived());
        if (dto.getEnablePartialPayment() != null) settings.setEnablePartialPayment(dto.getEnablePartialPayment());
        if (dto.getEnableInvoiceUpdated() != null) settings.setEnableInvoiceUpdated(dto.getEnableInvoiceUpdated());
        if (dto.getEnableAdvanceBalance() != null) settings.setEnableAdvanceBalance(dto.getEnableAdvanceBalance());
        if (dto.getEnablePaymentReminder() != null) settings.setEnablePaymentReminder(dto.getEnablePaymentReminder());
        if (dto.getReminderFrequencyDays() != null) settings.setReminderFrequencyDays(dto.getReminderFrequencyDays());
        if (dto.getEnablePoweredByMyBill() != null) settings.setEnablePoweredByMyBill(dto.getEnablePoweredByMyBill());

        CustomerNotificationSettings saved = settingsRepository.save(settings);
        return ResponseEntity.ok(toSettingsDTO(saved));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<CustomerNotificationTemplateDTO>> getTemplates() {
        Long userId = securityUtils.getCurrentUserId();

        // Get default and user customized templates
        List<CustomerNotificationTemplate> allTemplates = templateRepository.findAvailableTemplates(userId);

        // Map system defaults and overlay custom versions
        List<CustomerNotificationTemplateDTO> dtos = allTemplates.stream()
                .collect(Collectors.toMap(
                        t -> t.getTemplateType() + "_" + t.getChannel(),
                        t -> t,
                        // If both default and user-custom exist, keep user-custom (where user is not null)
                        (t1, t2) -> t1.getUser() != null ? t1 : t2
                ))
                .values()
                .stream()
                .map(t -> CustomerNotificationTemplateDTO.builder()
                        .id(t.getId())
                        .templateType(t.getTemplateType())
                        .channel(t.getChannel())
                        .subject(t.getSubject())
                        .messageBody(t.getMessageBody())
                        .isCustomized(t.getUser() != null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/templates")
    public ResponseEntity<CustomerNotificationTemplateDTO> updateTemplate(
            @RequestBody CustomerNotificationTemplateDTO dto
    ) {
        User user = securityUtils.getCurrentUser();

        // Find existing customized template for this user/type/channel
        CustomerNotificationTemplate template = templateRepository
                .findByUserIdAndTemplateTypeAndChannelAndIsDeletedFalse(user.getId(), dto.getTemplateType(), dto.getChannel())
                .orElse(null);

        if (template == null) {
            // Check if default system template exists
            CustomerNotificationTemplate defaultTemplate = templateRepository
                    .findByUserIdIsNullAndTemplateTypeAndChannelAndIsDeletedFalse(dto.getTemplateType(), dto.getChannel())
                    .orElseThrow(() -> new RuntimeException("Default template not found for type: " + dto.getTemplateType()));

            // Create customized template row for user override
            template = CustomerNotificationTemplate.builder()
                    .user(user)
                    .templateType(dto.getTemplateType())
                    .channel(dto.getChannel())
                    .subject(dto.getSubject() != null ? dto.getSubject() : defaultTemplate.getSubject())
                    .messageBody(dto.getMessageBody())
                    .build();
        } else {
            // Update user override template
            template.setMessageBody(dto.getMessageBody());
            if (dto.getSubject() != null) {
                template.setSubject(dto.getSubject());
            }
        }

        CustomerNotificationTemplate saved = templateRepository.save(template);

        return ResponseEntity.ok(CustomerNotificationTemplateDTO.builder()
                .id(saved.getId())
                .templateType(saved.getTemplateType())
                .channel(saved.getChannel())
                .subject(saved.getSubject())
                .messageBody(saved.getMessageBody())
                .isCustomized(true)
                .build());
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<CustomerNotificationLog>> getLogs(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<CustomerNotificationLog> logs = logRepository.findByUserIdOrderByCreatedTimeDesc(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    private CustomerNotificationSettingsDTO toSettingsDTO(CustomerNotificationSettings s) {
        return CustomerNotificationSettingsDTO.builder()
                .id(s.getId())
                .enableWhatsApp(s.getEnableWhatsApp())
                .enableInvoiceGenerated(s.getEnableInvoiceGenerated())
                .enablePaymentReceived(s.getEnablePaymentReceived())
                .enablePartialPayment(s.getEnablePartialPayment())
                .enableInvoiceUpdated(s.getEnableInvoiceUpdated())
                .enableAdvanceBalance(s.getEnableAdvanceBalance())
                .enablePaymentReminder(s.getEnablePaymentReminder())
                .reminderFrequencyDays(s.getReminderFrequencyDays())
                .enablePoweredByMyBill(s.getEnablePoweredByMyBill())
                .build();
    }
}
