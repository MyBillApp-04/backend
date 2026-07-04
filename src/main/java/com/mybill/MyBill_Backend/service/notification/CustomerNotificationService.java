package com.mybill.MyBill_Backend.service.notification;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.service.notification.channel.NotificationChannelProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final CustomerNotificationSettingsRepository settingsRepository;
    private final CustomerNotificationTemplateRepository templateRepository;
    private final CustomerNotificationLogRepository logRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final List<NotificationChannelProvider> channelProviders;

    /**
     * Resolves the appropriate settings for the user, creating default settings if none exist.
     */
    @Transactional
    public CustomerNotificationSettings getOrInitializeSettings(User user) {
        return settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    CustomerNotificationSettings newSettings = CustomerNotificationSettings.builder()
                            .user(user)
                            .enableWhatsApp(true)
                            .enableInvoiceGenerated(true)
                            .enablePaymentReceived(true)
                            .enablePartialPayment(true)
                            .enableInvoiceUpdated(true)
                            .enableAdvanceBalance(true)
                            .enablePaymentReminder(true)
                            .reminderFrequencyDays(7)
                            .enablePoweredByMyBill(true)
                            .build();
                    return settingsRepository.save(newSettings);
                });
    }

    /**
     * Sends a notification to a customer if enabled by the user's settings.
     */
    @Transactional
    public void processAndSendNotification(
            User user,
            Client customer,
            Invoice invoice,
            String type, // INVOICE_GENERATED, PAYMENT_RECEIVED, PARTIAL_PAYMENT, INVOICE_UPDATED, ADVANCE_BALANCE, PAYMENT_REMINDER
            Map<String, Object> contextValues
    ) {
        CustomerNotificationSettings settings = getOrInitializeSettings(user);

        // Check if the overall trigger type is enabled in the business settings
        if (!isTriggerEnabled(settings, type)) {
            log.info("Notification type {} is disabled for user {}", type, user.getEmail());
            return;
        }

        // Validate and normalize phone number
        String phone = customer.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("Cannot send notification. Customer {} has no phone number", customer.getName());
            return;
        }

        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.startsWith("0") && digitsOnly.length() > 10) {
            digitsOnly = digitsOnly.substring(1);
        }

        String normalizedPhone;
        if (digitsOnly.length() == 10) {
            normalizedPhone = "91" + digitsOnly; // Default to India country code prefix
        } else if (digitsOnly.length() < 10) {
            log.warn("Cannot send notification. Customer {} has an invalid phone number length: {}", customer.getName(), phone);
            return;
        } else {
            normalizedPhone = digitsOnly;
        }

        // Check for duplicates (only for Invoice triggers where we don't want duplicate sends)
        if (invoice != null && !"INVOICE_UPDATED".equals(type) && !"PAYMENT_REMINDER".equals(type)) {
            if (logRepository.existsByInvoiceIdAndNotificationTypeAndStatus(invoice.getId(), type, "SENT")) {
                log.info("Duplicate notification check: {} notification already sent for invoice ID {}", type, invoice.getId());
                return;
            }
        }

        // Determine enabled channels (for Phase 1, we support WhatsApp if enabled in settings)
        List<String> activeChannels = new ArrayList<>();
        if (Boolean.TRUE.equals(settings.getEnableWhatsApp())) {
            activeChannels.add("WHATSAPP");
        }

        if (activeChannels.isEmpty()) {
            log.info("No active notification channels enabled for user {}", user.getEmail());
            return;
        }

        for (String channel : activeChannels) {
            // Retrieve customized or default system template
            CustomerNotificationTemplate template = getTemplate(user.getId(), type, channel);
            if (template == null) {
                log.warn("No template found for type {} on channel {}", type, channel);
                continue;
            }

            // Create log entry in PENDING state
            CustomerNotificationLog notificationLog = CustomerNotificationLog.builder()
                    .user(user)
                    .customer(customer)
                    .invoice(invoice)
                    .phoneNumber(normalizedPhone)
                    .notificationType(type)
                    .channel(channel)
                    .status("PENDING")
                    .retryCount(0)
                    .build();

            notificationLog = logRepository.save(notificationLog);

            // Send notification
            sendWithLogUpdate(notificationLog, template, settings, customer, invoice, contextValues);
        }
    }

    /**
     * Executes the dispatch via the provider and records progress or logs failure.
     */
    public void sendWithLogUpdate(
            CustomerNotificationLog notificationLog,
            CustomerNotificationTemplate template,
            CustomerNotificationSettings settings,
            Client customer,
            Invoice invoice,
            Map<String, Object> contextValues
    ) {
        try {
            // Find provider
            NotificationChannelProvider provider = channelProviders.stream()
                    .filter(p -> p.getChannelName().equalsIgnoreCase(notificationLog.getChannel()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No provider registered for channel: " + notificationLog.getChannel()));

            // Render text template
            String rawBody = template.getMessageBody();
            String renderedBody = interpolateTemplate(rawBody, customer, invoice, settings, contextValues);

            // Invoke channel provider
            String response = provider.sendNotification(notificationLog.getPhoneNumber(), template.getSubject(), renderedBody);

            // Update log on success
            notificationLog.setStatus("SENT");
            notificationLog.setSentTime(LocalDateTime.now());
            notificationLog.setProviderResponse(response);
            logRepository.save(notificationLog);

        } catch (Exception e) {
            log.error("Failed to send customer notification: {}", e.getMessage(), e);

            // Update log on failure
            notificationLog.setStatus("FAILED");
            notificationLog.setFailureReason(e.getMessage());
            notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
            logRepository.save(notificationLog);
        }
    }

    /**
     * Retries failed notifications.
     */
    @Transactional
    public void retryFailedNotifications(int maxAttempts) {
        List<CustomerNotificationLog> failedLogs = logRepository.findByStatusAndRetryCountLessThan("FAILED", maxAttempts);
        if (failedLogs.isEmpty()) return;

        log.info("Found {} failed notifications for retry execution", failedLogs.size());

        for (CustomerNotificationLog logEntry : failedLogs) {
            logEntry.setStatus("RETRYING");
            logRepository.saveAndFlush(logEntry);

            CustomerNotificationSettings settings = getOrInitializeSettings(logEntry.getUser());
            CustomerNotificationTemplate template = getTemplate(logEntry.getUser().getId(), logEntry.getNotificationType(), logEntry.getChannel());

            if (template == null) {
                logEntry.setStatus("FAILED");
                logEntry.setFailureReason("Template missing for retry dispatch");
                logRepository.save(logEntry);
                continue;
            }

            // Create context values based on entity states
            Map<String, Object> contextValues = new HashMap<>();
            if (logEntry.getInvoice() != null) {
                Invoice inv = logEntry.getInvoice();
                contextValues.put("amount", inv.getRemainingAmount());
                contextValues.put("receivedAmount", inv.getPaidAmount());
                contextValues.put("remainingAmount", inv.getRemainingAmount());
                contextValues.put("paymentStatus", inv.getPaymentStatus().name());
            }

            sendWithLogUpdate(logEntry, template, settings, logEntry.getCustomer(), logEntry.getInvoice(), contextValues);
        }
    }

    private boolean isTriggerEnabled(CustomerNotificationSettings settings, String type) {
        return switch (type) {
            case "INVOICE_GENERATED" -> Boolean.TRUE.equals(settings.getEnableInvoiceGenerated());
            case "PAYMENT_RECEIVED" -> Boolean.TRUE.equals(settings.getEnablePaymentReceived());
            case "PARTIAL_PAYMENT" -> Boolean.TRUE.equals(settings.getEnablePartialPayment());
            case "INVOICE_UPDATED" -> Boolean.TRUE.equals(settings.getEnableInvoiceUpdated());
            case "ADVANCE_BALANCE" -> Boolean.TRUE.equals(settings.getEnableAdvanceBalance());
            case "PAYMENT_REMINDER" -> Boolean.TRUE.equals(settings.getEnablePaymentReminder());
            default -> false;
        };
    }

    private CustomerNotificationTemplate getTemplate(Long userId, String type, String channel) {
        return templateRepository.findByUserIdAndTemplateTypeAndChannelAndIsDeletedFalse(userId, type, channel)
                .orElseGet(() -> templateRepository.findByUserIdIsNullAndTemplateTypeAndChannelAndIsDeletedFalse(type, channel)
                        .orElse(null));
    }

    private String interpolateTemplate(
            String template,
            Client customer,
            Invoice invoice,
            CustomerNotificationSettings settings,
            Map<String, Object> contextValues
    ) {
        if (template == null) return "";

        String result = template;

        // Fetch business details
        BusinessProfile profile = businessProfileRepository.findByUserId(settings.getUser().getId()).orElse(null);
        String businessName = profile != null ? profile.getBusinessName() : "MyBill Workspace";
        String businessPhone = profile != null ? profile.getPhone() : "";
        String businessAddress = profile != null ? profile.getAddress() : "";

        // Standard place-holders
        result = result.replace("{{customerName}}", customer.getName() != null ? customer.getName() : "");
        result = result.replace("{{businessName}}", businessName);
        result = result.replace("{{businessPhone}}", businessPhone != null ? businessPhone : "");
        result = result.replace("{{businessAddress}}", businessAddress != null ? businessAddress : "");

        if (invoice != null) {
            result = result.replace("{{invoiceNumber}}", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
            if (invoice.getInvoiceDate() != null) {
                result = result.replace("{{invoiceDate}}", invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            result = result.replace("{{paymentStatus}}", invoice.getPaymentStatus() != null ? invoice.getPaymentStatus().name() : "");
            result = result.replace("{{amount}}", String.format("%.2f", invoice.getTotalAmount()));
        }

        // Contextual dynamic variables
        if (contextValues != null) {
            for (Map.Entry<String, Object> entry : contextValues.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue() instanceof Double || entry.getValue() instanceof Float) {
                        result = result.replace("{{" + entry.getKey() + "}}", String.format("%.2f", entry.getValue()));
                    } else {
                        result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
                    }
                }
            }
        }

        // Clean out unmatched braces
        result = result.replaceAll("\\{\\{\\w+\\}\\}", "");

        // Add powered by footer if configured
        if (Boolean.TRUE.equals(settings.getEnablePoweredByMyBill())) {
            result += "\n\nGenerated using MyBill.";
        }

        return result;
    }
}
