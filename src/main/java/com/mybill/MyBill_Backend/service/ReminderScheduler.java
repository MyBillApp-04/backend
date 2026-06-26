package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.PaymentStatus;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    @Value("${app.email.reminders.enabled:true}")
    private boolean remindersEnabled;

    @Scheduled(fixedDelayString = "${app.email.retry.delay-ms:300000}")
    public void retryFailedEmails() {
        emailService.retryFailedEmails();
    }

    @Scheduled(cron = "${app.email.reminders.cron:0 0 9 * * *}")
    public void sendDuePaymentReminders() {
        if (!remindersEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        invoiceRepository
                .findTop50ByIsDeletedFalseAndPaymentStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                        PaymentStatus.PAID,
                        now.plusDays(1)
                )
                .forEach(this::sendReminder);
    }

    private void sendReminder(Invoice invoice) {
        if (invoice.getClient() == null || invoice.getClient().getUser() == null) {
            return;
        }

        String recipient = invoice.getClient().getEmail();
        if (recipient == null || !recipient.contains("@")) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean overdue = invoice.getDueDate() != null && invoice.getDueDate().isBefore(now);
        String templateType = overdue ? "OVERDUE_REMINDER" : "DUE_REMINDER";

        if (emailService.reminderSentRecently(invoice.getId(), templateType, now.minusHours(20))) {
            return;
        }

        emailService.sendPaymentReminder(invoice, recipient, templateType);
    }
}
