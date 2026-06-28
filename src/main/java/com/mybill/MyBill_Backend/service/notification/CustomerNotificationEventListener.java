package com.mybill.MyBill_Backend.service.notification;

import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.event.AdvanceBalanceAvailableEvent;
import com.mybill.MyBill_Backend.event.InvoiceCreatedEvent;
import com.mybill.MyBill_Backend.event.InvoiceUpdatedEvent;
import com.mybill.MyBill_Backend.event.PaymentRecordedEvent;
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
        log.info("Handling InvoiceCreatedEvent for invoice: {}", invoice.getInvoiceNumber());

        Map<String, Object> context = new HashMap<>();
        context.put("amount", invoice.getTotalAmount());
        context.put("remainingAmount", invoice.getRemainingAmount());
        context.put("paymentStatus", invoice.getPaymentStatus().name());

        notificationService.processAndSendNotification(
                invoice.getUser(),
                invoice.getClient(),
                invoice,
                "INVOICE_GENERATED",
                context
        );
    }

    @Async
    @EventListener
    public void handleInvoiceUpdated(InvoiceUpdatedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Handling InvoiceUpdatedEvent for invoice: {}", invoice.getInvoiceNumber());

        Map<String, Object> context = new HashMap<>();
        context.put("amount", invoice.getTotalAmount());
        context.put("remainingAmount", invoice.getRemainingAmount());
        context.put("paymentStatus", invoice.getPaymentStatus().name());

        notificationService.processAndSendNotification(
                invoice.getUser(),
                invoice.getClient(),
                invoice,
                "INVOICE_UPDATED",
                context
        );
    }

    @Async
    @EventListener
    public void handlePaymentRecorded(PaymentRecordedEvent event) {
        Invoice invoice = event.getInvoice();
        log.info("Handling PaymentRecordedEvent for amount: {}", event.getAmount());

        Map<String, Object> context = new HashMap<>();
        context.put("receivedAmount", event.getAmount());
        context.put("remainingAmount", event.getRemainingAmount());

        String type = event.getRemainingAmount() <= 0 ? "PAYMENT_RECEIVED" : "PARTIAL_PAYMENT";

        notificationService.processAndSendNotification(
                event.getPayment().getUser(),
                event.getPayment().getClient(),
                invoice,
                type,
                context
        );
    }

    @Async
    @EventListener
    public void handleAdvanceBalanceAvailable(AdvanceBalanceAvailableEvent event) {
        log.info("Handling AdvanceBalanceAvailableEvent for client: {}", event.getClient().getName());

        Map<String, Object> context = new HashMap<>();
        context.put("advanceAmount", event.getAdvanceAmount());

        notificationService.processAndSendNotification(
                event.getUser(),
                event.getClient(),
                null,
                "ADVANCE_BALANCE",
                context
        );
    }
}
