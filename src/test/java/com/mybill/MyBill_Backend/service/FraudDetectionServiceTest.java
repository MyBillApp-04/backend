package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.observability.AppMetrics;
import com.mybill.MyBill_Backend.repository.FraudCheckRepository;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FraudDetectionServiceTest {

    private FraudCheckRepository fraudCheckRepository;
    private PaymentRepository paymentRepository;
    private FraudDetectionService service;

    private User testUser;
    private Client testClient;

    @BeforeEach
    void setUp() {
        fraudCheckRepository = mock(FraudCheckRepository.class);
        paymentRepository = mock(PaymentRepository.class);

        AppMetrics appMetrics = mock(AppMetrics.class);
        Counter noopCounter = mock(Counter.class);
        when(appMetrics.getFraudFlagged()).thenReturn(noopCounter);
        when(appMetrics.getFraudFraudulent()).thenReturn(noopCounter);
        doNothing().when(noopCounter).increment();

        service = new FraudDetectionService(fraudCheckRepository, paymentRepository, appMetrics);

        ReflectionTestUtils.setField(service, "amountThreshold", 10000.0);
        ReflectionTestUtils.setField(service, "velocityLimit", 5);
        ReflectionTestUtils.setField(service, "velocityWindowMinutes", 10);

        testUser = User.builder().id(1L).email("user@example.com").build();
        testClient = Client.builder().id(UUID.randomUUID()).name("Acme Corp").user(testUser).build();

        when(fraudCheckRepository.save(any(FraudCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void evaluatePayment_SafePayment() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(500.0)
                .paymentMode(PaymentMode.UPI)
                .notes("Regular invoice payment")
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(1L); // only 1 payment within window

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isZero();
        assertThat(result.getStatus()).isEqualTo(FraudStatus.SAFE);
        assertThat(result.getRulesTriggered()).isEmpty();
    }

    @Test
    void evaluatePayment_HighAmount() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(15000.0)
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .notes("Large corporate layout")
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(1L);

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(40.0);
        assertThat(result.getStatus()).isEqualTo(FraudStatus.SUSPICIOUS);
        assertThat(result.getRulesTriggered()).contains("HIGH_AMOUNT");
    }

    @Test
    void evaluatePayment_HighVelocity() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(100.0)
                .paymentMode(PaymentMode.UPI)
                .notes("API webhook trigger")
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(7L); // velocityLimit is 5

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(30.0);
        assertThat(result.getStatus()).isEqualTo(FraudStatus.SUSPICIOUS);
        assertThat(result.getRulesTriggered()).contains("HIGH_VELOCITY");
    }

    @Test
    void evaluatePayment_CriticalVelocity() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(100.0)
                .paymentMode(PaymentMode.UPI)
                .notes("API webhook trigger")
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(12L); // velocityLimit*2 is 10

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(60.0);
        assertThat(result.getStatus()).isEqualTo(FraudStatus.FLAGGED);
        assertThat(result.getRulesTriggered()).contains("CRITICAL_VELOCITY");
    }

    @Test
    void evaluatePayment_SuspiciousNotes() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(500.0)
                .paymentMode(PaymentMode.UPI)
                .notes("This is a test payment requesting a chargeback refund")
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(1L);

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        // notes contains: "test", "chargeback", "refund" -> 3 matches -> 3 * 20 = 60, capped at 40
        assertThat(result.getScore()).isEqualTo(40.0);
        assertThat(result.getStatus()).isEqualTo(FraudStatus.SUSPICIOUS);
        assertThat(result.getRulesTriggered()).contains("SUSPICIOUS_NOTES");
    }

    @Test
    void evaluatePayment_MultipleIndicators() {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .amount(25000.0) // high amount (40 pts)
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .notes("URGENT: hack bypass test refund") // suspicious keywords (40 pts)
                .user(testUser)
                .client(testClient)
                .date(LocalDateTime.now())
                .build();

        when(paymentRepository.countByUserIdAndDateAfterAndIsDeletedFalse(eq(1L), any(LocalDateTime.class)))
                .thenReturn(15L); // critical velocity (60 pts)

        FraudCheck result = service.evaluatePayment(payment);

        assertThat(result).isNotNull();
        // 40 + 40 + 60 = 140 pts
        assertThat(result.getScore()).isEqualTo(140.0);
        assertThat(result.getStatus()).isEqualTo(FraudStatus.FRAUDULENT);
        assertThat(result.getRulesTriggered()).contains("HIGH_AMOUNT", "CRITICAL_VELOCITY", "SUSPICIOUS_NOTES");
    }
}
