package com.mybill.MyBill_Backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Central registry of custom business and application metrics.
 *
 * <p>Counters and timers defined here are automatically scraped by the
 * {@code /actuator/prometheus} endpoint and are available in Prometheus /
 * Grafana dashboards.</p>
 */
@Component
@Getter
public class AppMetrics {

    // ── Payment metrics ────────────────────────────────────────────────────────
    private final Counter paymentsReceived;
    private final Counter paymentFailures;
    private final Timer paymentProcessingTime;

    // ── Invoice metrics ────────────────────────────────────────────────────────
    private final Counter invoicesGenerated;
    private final Counter invoicesFullyPaid;

    // ── Fraud metrics ──────────────────────────────────────────────────────────
    private final Counter fraudFlagged;
    private final Counter fraudFraudulent;

    // ── Authentication metrics ─────────────────────────────────────────────────
    private final Counter authSuccesses;
    private final Counter authFailures;

    // ── Data retention metrics ─────────────────────────────────────────────────
    private final Counter retentionRecordsPruned;

    public AppMetrics(MeterRegistry registry) {
        paymentsReceived = Counter.builder("mybill.payments.received.total")
                .description("Total number of payments received")
                .register(registry);

        paymentFailures = Counter.builder("mybill.payments.failures.total")
                .description("Total number of failed payment processing attempts")
                .register(registry);

        paymentProcessingTime = Timer.builder("mybill.payments.processing.duration")
                .description("Time taken to process a payment")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(false)
                .register(registry);

        invoicesGenerated = Counter.builder("mybill.invoices.generated.total")
                .description("Total invoices generated")
                .register(registry);

        invoicesFullyPaid = Counter.builder("mybill.invoices.fully_paid.total")
                .description("Total invoices that became fully paid")
                .register(registry);

        fraudFlagged = Counter.builder("mybill.fraud.flagged.total")
                .description("Payments flagged as suspicious or above")
                .tag("severity", "flagged")
                .register(registry);

        fraudFraudulent = Counter.builder("mybill.fraud.fraudulent.total")
                .description("Payments classified as fraudulent")
                .tag("severity", "fraudulent")
                .register(registry);

        authSuccesses = Counter.builder("mybill.auth.success.total")
                .description("Successful authentication events")
                .register(registry);

        authFailures = Counter.builder("mybill.auth.failure.total")
                .description("Failed authentication events")
                .register(registry);

        retentionRecordsPruned = Counter.builder("mybill.retention.pruned.total")
                .description("Records pruned by the GDPR data retention scheduler")
                .register(registry);
    }

    /** Record a payment processing duration in milliseconds. */
    public void recordPaymentDuration(long durationMs) {
        paymentProcessingTime.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public Counter getPaymentsReceived() { return paymentsReceived; }
    public Counter getPaymentFailures() { return paymentFailures; }
    public Timer getPaymentProcessingTime() { return paymentProcessingTime; }
    public Counter getInvoicesGenerated() { return invoicesGenerated; }
    public Counter getInvoicesFullyPaid() { return invoicesFullyPaid; }
    public Counter getFraudFlagged() { return fraudFlagged; }
    public Counter getFraudFraudulent() { return fraudFraudulent; }
    public Counter getAuthSuccesses() { return authSuccesses; }
    public Counter getAuthFailures() { return authFailures; }
    public Counter getRetentionRecordsPruned() { return retentionRecordsPruned; }
}
