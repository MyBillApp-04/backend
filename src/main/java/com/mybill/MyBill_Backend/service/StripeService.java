package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class StripeService {

    @CircuitBreaker(name = "stripeService", fallbackMethod = "fallbackProcessPayment")
    public void processPayment(String paymentIntentId, Double amount) throws Exception {
        log.info("Contacting Stripe API for paymentIntentPresent={} amountPresent={}",
                paymentIntentId != null && !paymentIntentId.isBlank(), amount != null);

        if (paymentIntentId != null && (paymentIntentId.contains("fail") || paymentIntentId.contains("error"))) {
            log.error("Simulated Stripe network/API failure for paymentIntentPresent={}", true);
            throw new IOException("Stripe gateway timeout calling POST /v1/payment_intents/" + paymentIntentId);
        }

        log.info("Stripe payment processed successfully");
    }

    public void fallbackProcessPayment(String paymentIntentId, Double amount, Throwable t) throws Exception {
        log.warn("Stripe processPayment fallback triggered: exception={} message={}",
                t.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(t.getMessage()));
        throw new RuntimeException("Stripe API unavailable. Detail: " + t.getMessage(), t);
    }
}
