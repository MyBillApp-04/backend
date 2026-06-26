package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) throws Exception {
        Event event = constructEvent(payload, signature);

        Object eventObject = event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if ("payment_intent.succeeded".equals(event.getType()) && eventObject instanceof PaymentIntent intent) {
            paymentService.verifyWebhookPayment(intent);
        } else if (("payment_intent.payment_failed".equals(event.getType())
                || "payment_intent.canceled".equals(event.getType()))
                && eventObject instanceof PaymentIntent intent) {
            paymentService.updateWebhookIntentStatus(intent);
        } else if ("charge.refunded".equals(event.getType()) && eventObject instanceof Charge charge) {
            paymentService.applyWebhookRefund(
                    charge.getPaymentIntent(),
                    charge.getAmountRefunded(),
                    charge.getRefunded() ? "refunded" : charge.getStatus()
            );
        }

        return ResponseEntity.ok().build();
    }

    private Event constructEvent(String payload, String signature) throws SignatureVerificationException {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new SignatureVerificationException("Stripe webhook secret is not configured", signature);
        }

        return Webhook.constructEvent(payload, signature, webhookSecret);
    }
}
