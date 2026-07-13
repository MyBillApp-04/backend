package com.mybill.MyBill_Backend.service.notification;

import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.event.AdvanceBalanceAvailableEvent;
import com.mybill.MyBill_Backend.event.InvoiceCreatedEvent;
import com.mybill.MyBill_Backend.event.InvoiceUpdatedEvent;
import com.mybill.MyBill_Backend.event.PaymentRecordedEvent;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerNotificationEventListener {

    private final CustomerNotificationService notificationService;

    @Async
    @EventListener
    public void handleInvoiceCreated(InvoiceCreatedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Handling InvoiceCreatedEvent for invoice ID {}", invoice.getId());

        // Skip sending automated alerts for backdated/historical synced invoices (created more than 24 hours ago)
        if (invoice.getInvoiceDate() != null && invoice.getInvoiceDate().isBefore(java.time.LocalDateTime.now().minusDays(1))) {
            log.info("Skipping automated notification trigger for historical synced invoice ID {}", invoice.getId());
            return;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("amount", invoice.getTotalAmount());
        context.put("remainingAmount", invoice.getRemainingAmount());
        context.put("paymentStatus", invoice.getPaymentStatus().name());

        try {
            notificationService.processAndSendNotification(
                    invoice.getUser(),
                    invoice.getClient(),
                    invoice,
                    "INVOICE_GENERATED",
                    context
            );
        } catch (Exception e) {
            log.error("Failed to send customer notification for invoice creation: invoiceId={} exception={} message={}. Continuing gracefully.",
                    invoice.getId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
        }
    }

    @Async
    @EventListener
    public void handleInvoiceUpdated(InvoiceUpdatedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Handling InvoiceUpdatedEvent for invoice ID {}", invoice.getId());

        Map<String, Object> context = new HashMap<>();
        context.put("amount", invoice.getTotalAmount());
        context.put("remainingAmount", invoice.getRemainingAmount());
        context.put("paymentStatus", invoice.getPaymentStatus().name());

        try {
            notificationService.processAndSendNotification(
                    invoice.getUser(),
                    invoice.getClient(),
                    invoice,
                    "INVOICE_UPDATED",
                    context
            );
        } catch (Exception e) {
            log.error("Failed to send customer notification for invoice update: invoiceId={} exception={} message={}. Continuing gracefully.",
                    invoice.getId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
        }
    }

    @Async
    @EventListener
    public void handlePaymentRecorded(PaymentRecordedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Handling PaymentRecordedEvent for invoice ID {}", invoice.getId());

        Map<String, Object> context = new HashMap<>();
        context.put("receivedAmount", event.getAmount());
        context.put("remainingAmount", event.getRemainingAmount());

        String type = event.getRemainingAmount() <= 0 ? "PAYMENT_RECEIVED" : "PARTIAL_PAYMENT";

        try {
            notificationService.processAndSendNotification(
                    event.getPayment().getUser(),
                    event.getPayment().getClient(),
                    invoice,
                    type,
                    context
            );
        } catch (Exception e) {
            log.error("Failed to send customer notification for payment recorded: invoiceId={} exception={} message={}. Continuing gracefully.",
                    invoice.getId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
        }
    }

    @Async
    @EventListener
    public void handleAdvanceBalanceAvailable(AdvanceBalanceAvailableEvent event) {
        log.info("Handling AdvanceBalanceAvailableEvent for client ID {}", event.getClient().getId());

        Map<String, Object> context = new HashMap<>();
        context.put("advanceAmount", event.getAdvanceAmount());

        try {
            notificationService.processAndSendNotification(
                    event.getUser(),
                    event.getClient(),
                    null,
                    "ADVANCE_BALANCE",
                    context
            );
        } catch (Exception e) {
            log.error("Failed to send customer notification for advance balance availability: clientId={} exception={} message={}. Continuing gracefully.",
                    event.getClient().getId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
        }
    }
}
