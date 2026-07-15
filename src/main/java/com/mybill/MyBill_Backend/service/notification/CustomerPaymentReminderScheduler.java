package com.mybill.MyBill_Backend.service.notification;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.CustomerNotificationLogRepository;
import com.mybill.MyBill_Backend.service.DatabaseLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduling.enabled", havingValue = "true", matchIfMissing = false)
public class CustomerPaymentReminderScheduler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerNotificationLogRepository logRepository;
    private final CustomerNotificationService notificationService;
    private final DatabaseLockService databaseLockService;
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Value("${app.notifications.reminders.batch-size:100}")
    private int reminderBatchSize;

    @Scheduled(cron = "${app.notifications.reminders.cron:0 0 9 * * *}")
    public void sendPaymentReminders() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping customer payment reminder run because the previous run is still active");
            return;
        }
        if (!databaseLockService.tryLock(DatabaseLockService.CUSTOMER_PAYMENT_REMINDER)) {
            log.info("Skipping customer payment reminder run because another instance owns the database scheduler lock");
            running.set(false);
            return;
        }

        try {
        log.info("Starting scheduled task for customer payment reminders");

        List<Invoice> activeDueInvoices = invoiceRepository.findByIsDeletedFalseAndPaymentStatusIn(
                List.of(PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID),
                PageRequest.of(0, Math.max(1, reminderBatchSize))
        ).getContent();

        if (activeDueInvoices.isEmpty()) {
            log.info("No unpaid or partially paid invoices found for reminders");
            return;
        }

        log.info("Processing {} unpaid or partially paid invoices for reminders", activeDueInvoices.size());
        LocalDateTime now = LocalDateTime.now();

        // Group by user/business owner to query settings efficiently
        Map<Long, List<Invoice>> userInvoiceMap = new HashMap<>();
        for (Invoice invoice : activeDueInvoices) {
            if (invoice.getUser() != null) {
                userInvoiceMap.computeIfAbsent(invoice.getUser().getId(), k -> new ArrayList<>()).add(invoice);
            }
        }

        for (Map.Entry<Long, List<Invoice>> entry : userInvoiceMap.entrySet()) {
            Long userId = entry.getKey();
            List<Invoice> invoices = entry.getValue();

            User user = invoices.get(0).getUser();
            CustomerNotificationSettings settings = notificationService.getOrInitializeSettings(user);

            if (!Boolean.TRUE.equals(settings.getEnablePaymentReminder())) {
                log.info("Payment reminders are disabled for user ID {}", userId);
                continue;
            }

            int frequencyDays = settings.getReminderFrequencyDays() != null ? settings.getReminderFrequencyDays() : 7;
            log.info("User ID {} reminder frequency: {} days", userId, frequencyDays);

            for (Invoice invoice : invoices) {
                if (invoice.getClient() == null) continue;

                LocalDateTime effectiveDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : invoice.getCreatedDate();
                if (effectiveDate == null) continue;

                // Ensure the invoice is at least frequencyDays old before sending first reminder
                long daysSinceCreation = Duration.between(effectiveDate, now).toDays();
                if (daysSinceCreation < frequencyDays) {
                    continue;
                }

                // Check when the last reminder was successfully sent
                LocalDateTime lastSent = logRepository.findLastSentTimeByInvoiceIdAndType(invoice.getId(), "PAYMENT_REMINDER");

                if (lastSent == null || Duration.between(lastSent, now).toDays() >= frequencyDays) {
                    log.info("Triggering payment reminder for invoice ID {} and client ID {}",
                            invoice.getId(), invoice.getClient().getId());
                    
                    Map<String, Object> context = new HashMap<>();
                    context.put("amount", invoice.getRemainingAmount());
                    context.put("remainingAmount", invoice.getRemainingAmount());
                    context.put("paymentStatus", invoice.getPaymentStatus().name());

                    notificationService.processAndSendNotification(
                            user,
                            invoice.getClient(),
                            invoice,
                            "PAYMENT_REMINDER",
                            context
                    );
                }
            }
        }
        } finally {
            databaseLockService.unlock(DatabaseLockService.CUSTOMER_PAYMENT_REMINDER);
            running.set(false);
        }
    }
}
