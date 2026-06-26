package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.Payment;
import com.mybill.MyBill_Backend.entity.PaymentMode;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceWebhookTest {

    private PaymentRepository paymentRepository;
    private InvoiceService invoiceService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        invoiceService = mock(InvoiceService.class);
        paymentService = new PaymentService(paymentRepository, invoiceService, mock(SecurityUtils.class));
    }

    @Test
    void verifyWebhookPaymentAppliesSuccessfulIntentOnceForStoredOwner() {
        UUID invoiceId = UUID.randomUUID();
        Payment payment = paymentFor(invoiceId, 7L);
        PaymentIntent intent = intentFor("pi_verified", "succeeded", "ch_verified", 7L, invoiceId);

        when(paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse("pi_verified"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment verified = paymentService.verifyWebhookPayment(intent);

        assertThat(verified.getAppliedToInvoice()).isTrue();
        assertThat(verified.getStripeStatus()).isEqualTo("succeeded");
        assertThat(verified.getStripeChargeId()).isEqualTo("ch_verified");
        verify(invoiceService).addPaymentForUser(
                eq(invoiceId),
                eq(7L),
                eq(125.50),
                eq(PaymentMode.STRIPE),
                any(LocalDateTime.class)
        );
        verify(paymentRepository).save(payment);

        paymentService.verifyWebhookPayment(intent);

        verify(invoiceService).addPaymentForUser(
                eq(invoiceId),
                eq(7L),
                eq(125.50),
                eq(PaymentMode.STRIPE),
                any(LocalDateTime.class)
        );
    }

    @Test
    void verifyWebhookPaymentRejectsMismatchedWebhookOwnership() {
        UUID invoiceId = UUID.randomUUID();
        Payment payment = paymentFor(invoiceId, 7L);
        PaymentIntent intent = intentFor("pi_mismatch", "succeeded", "ch_mismatch", 99L, invoiceId);

        when(paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse("pi_mismatch"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyWebhookPayment(intent))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("ownership");

        verify(invoiceService, never()).addPaymentForUser(any(), any(), any(), any(), any());
        verify(paymentRepository, never()).save(any());
    }

    private static Payment paymentFor(UUID invoiceId, Long userId) {
        User user = new User();
        user.setId(userId);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setUser(user);

        return Payment.builder()
                .paymentId(UUID.randomUUID())
                .invoice(invoice)
                .user(user)
                .amount(125.50)
                .paymentMode(PaymentMode.STRIPE)
                .date(LocalDateTime.now())
                .stripePaymentIntentId("pi_verified")
                .appliedToInvoice(false)
                .build();
    }

    private static PaymentIntent intentFor(String id, String status, String chargeId, Long userId, UUID invoiceId) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(id);
        intent.setStatus(status);
        intent.setLatestCharge(chargeId);
        intent.setMetadata(Map.of(
                "userId", userId.toString(),
                "invoiceId", invoiceId.toString()
        ));
        return intent;
    }
}
