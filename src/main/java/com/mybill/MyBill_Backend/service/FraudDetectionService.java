package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.FraudCheckRepository;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final FraudCheckRepository fraudCheckRepository;
    private final PaymentRepository paymentRepository;
    private final com.mybill.MyBill_Backend.observability.AppMetrics appMetrics;

    @Value("${app.fraud.amount-threshold:10000.0}")
    private double amountThreshold;

    @Value("${app.fraud.velocity-limit:5}")
    private int velocityLimit;

    @Value("${app.fraud.velocity-window-minutes:10}")
    private int velocityWindowMinutes;

    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "chargeback", "refund", "hack", "test", "fraud", "spoof", "bypass"
    );

    @Transactional
    public FraudCheck evaluatePayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null for fraud evaluation");
        }

        double score = 0.0;
        List<String> rulesTriggered = new ArrayList<>();
        StringBuilder notesBuilder = new StringBuilder();

        // 1. High Amount Check
        double amount = payment.getAmount() != null ? payment.getAmount() : 0.0;
        if (amount > amountThreshold) {
            score += 40.0;
            rulesTriggered.add("HIGH_AMOUNT");
            notesBuilder.append(String.format("Payment amount %.2f exceeds threshold %.2f. ", amount, amountThreshold));
        }

        // 2. Velocity Check
        if (payment.getUser() != null && payment.getUser().getId() != null) {
            Long userId = payment.getUser().getId();
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(velocityWindowMinutes);
            long velocityCount = paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(userId, windowStart);

            if (velocityCount > velocityLimit * 2) {
                score += 60.0;
                rulesTriggered.add("CRITICAL_VELOCITY");
                notesBuilder.append(String.format("Critical payment frequency detected: %d payments in last %d minutes. ", velocityCount, velocityWindowMinutes));
            } else if (velocityCount > velocityLimit) {
                score += 30.0;
                rulesTriggered.add("HIGH_VELOCITY");
                notesBuilder.append(String.format("High payment frequency detected: %d payments in last %d minutes. ", velocityCount, velocityWindowMinutes));
            }
        }

        // 3. Suspicious Notes Check
        String notes = payment.getNotes();
        if (notes != null && !notes.isBlank()) {
            String lowercaseNotes = notes.toLowerCase(Locale.ROOT);
            int matches = 0;
            for (String keyword : SUSPICIOUS_KEYWORDS) {
                if (lowercaseNotes.contains(keyword)) {
                    matches++;
                }
            }
            if (matches > 0) {
                double noteScore = Math.min(matches * 20.0, 40.0);
                score += noteScore;
                rulesTriggered.add("SUSPICIOUS_NOTES");
                notesBuilder.append(String.format("Notes contain suspicious keywords (matched %d keywords). ", matches));
            }
        }

        // Classify status
        FraudStatus status;
        if (score >= 80.0) {
            status = FraudStatus.FRAUDULENT;
        } else if (score >= 50.0) {
            status = FraudStatus.FLAGGED;
        } else if (score >= 25.0) {
            status = FraudStatus.SUSPICIOUS;
        } else {
            status = FraudStatus.SAFE;
        }

        String finalNotes = notesBuilder.toString().trim();
        if (finalNotes.isEmpty()) {
            finalNotes = "No fraud indicators triggered.";
        }

        FraudCheck check = FraudCheck.builder()
                .payment(payment)
                .user(payment.getUser())
                .score(score)
                .status(status)
                .rulesTriggered(String.join(",", rulesTriggered))
                .notes(finalNotes)
                .build();

        log.info("Fraud evaluation complete for Payment ID {}: score={}, status={}, rules={}",
                payment.getPaymentId(), score, status, check.getRulesTriggered());

        FraudCheck saved = fraudCheckRepository.save(check);

        if (status == FraudStatus.FRAUDULENT) {
            appMetrics.getFraudFraudulent().increment();
            appMetrics.getFraudFlagged().increment();
        } else if (status == FraudStatus.FLAGGED || status == FraudStatus.SUSPICIOUS) {
            appMetrics.getFraudFlagged().increment();
        }

        return saved;
    }
}