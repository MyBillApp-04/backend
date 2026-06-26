package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.PaymentIntentRequest;
import com.mybill.MyBill_Backend.dto.PaymentIntentResponse;
import com.mybill.MyBill_Backend.dto.PaymentSheetRequest;
import com.mybill.MyBill_Backend.dto.PaymentSheetResponse;
import com.mybill.MyBill_Backend.dto.RefundRequest;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final SecurityUtils securityUtils;

    @Value("${stripe.currency:inr}")
    private String defaultCurrency;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId());
        double amount = request.getAmount() != null ? request.getAmount() : remainingAmount(invoice);

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        double remaining = remainingAmount(invoice);
        if (amount > remaining) {
            throw new IllegalArgumentException("Payment amount cannot exceed remaining invoice balance");
        }

        String currency = normalizeCurrency(request.getCurrency());
        long amountMinor = toMinorUnit(amount);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinor)
                .setCurrency(currency)
                .putMetadata("invoiceId", invoice.getId().toString())
                .putMetadata("userId", securityUtils.getCurrentUserId().toString())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = Payment.builder()
                .client(invoice.getClient())
                .invoice(invoice)
                .user(securityUtils.getCurrentUser())
                .amount(amount)
                .paymentMode(PaymentMode.STRIPE)
                .date(LocalDateTime.now())
                .stripePaymentIntentId(intent.getId())
                .stripeStatus(intent.getStatus())
                .refundedAmount(0.0)
                .notes("Stripe payment intent created")
                .build();

        Payment saved = paymentRepository.save(payment);

        return PaymentIntentResponse.builder()
                .paymentId(saved.getPaymentId())
                .paymentIntentId(intent.getId())
                .clientSecret(intent.getClientSecret())
                .amount(amount)
                .currency(currency)
                .status(intent.getStatus())
                .build();
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PaymentSheetResponse createPaymentSheet(PaymentSheetRequest request) throws StripeException {
        Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId());
        double amount = request.getAmount() != null ? request.getAmount() : remainingAmount(invoice);

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        double remaining = remainingAmount(invoice);
        if (amount > remaining) {
            throw new IllegalArgumentException("Payment amount cannot exceed remaining invoice balance");
        }

        String currency = normalizeCurrency(request.getCurrency());
        long amountMinor = toMinorUnit(amount);

        // Get or create customer
        String customerId = getOrCreateCustomerId();

        // Create ephemeral key
        EphemeralKeyCreateParams ephemeralParams = EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion("2023-10-16") // Use a compatible Stripe API version
                .build();
        EphemeralKey ephemeralKey = EphemeralKey.create(ephemeralParams);

        // Create payment intent
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinor)
                .setCurrency(currency)
                .setCustomer(customerId)
                .putMetadata("invoiceId", invoice.getId().toString())
                .putMetadata("invoiceNumber", request.getInvoiceNumber())
                .putMetadata("userId", securityUtils.getCurrentUserId().toString())
                .putMetadata("clientName", request.getClientName())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // Create payment record
        Payment payment = Payment.builder()
                .client(invoice.getClient())
                .invoice(invoice)
                .user(securityUtils.getCurrentUser())
                .amount(amount)
                .paymentMode(PaymentMode.STRIPE)
                .date(LocalDateTime.now())
                .stripePaymentIntentId(intent.getId())
                .stripeStatus(intent.getStatus())
                .refundedAmount(0.0)
                .notes("Stripe payment sheet initialized")
                .build();

        paymentRepository.save(payment);

        return PaymentSheetResponse.builder()
                .clientSecret(intent.getClientSecret())
                .customerId(customerId)
                .ephemeralKey(ephemeralKey.getSecret())
                .publishableKey(publishableKey)
                .amount(amount)
                .currency(currency)
                .build();
    }

    private String getOrCreateCustomerId() throws StripeException {
        Long userId = securityUtils.getCurrentUserId();
        User currentUser = securityUtils.getCurrentUser();

        // Check if customer already exists by searching metadata
        // For simplicity, we create a new customer per payment intent
        // In production, you might want to store Stripe customer ID in the User entity
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(currentUser.getEmail())
                .setName(currentUser.getName())
                .putMetadata("userId", userId.toString())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Payment verifyPaymentForCurrentUser(String paymentIntentId) throws StripeException {
        Long userId = securityUtils.getCurrentUserId();
        Payment payment = paymentRepository.findByStripePaymentIntentIdAndUserIdAndIsDeletedFalse(paymentIntentId, userId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent"));

        return verifyPayment(paymentIntentId, payment, userId);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Payment verifyWebhookPayment(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse(intent.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found for intent"));

        validateWebhookOwnership(intent, payment);

        return applyVerifiedIntent(intent, payment, payment.getUser().getId());
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Payment updateWebhookIntentStatus(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse(intent.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found for intent"));

        validateWebhookOwnership(intent, payment);
        payment.setStripeStatus(intent.getStatus());
        payment.setNotes("Stripe payment intent status updated: " + intent.getStatus());

        return paymentRepository.save(payment);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Payment applyWebhookRefund(String paymentIntentId, Long amountRefundedMinor, String status) {
        Payment payment = paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for refund event"));

        double providerRefundedAmount = fromMinorUnit(amountRefundedMinor);
        double previousRefundedAmount = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0;
        double delta = Math.max(providerRefundedAmount - previousRefundedAmount, 0.0);

        payment.setStripeStatus(status);
        payment.setRefundedAmount(Math.min(providerRefundedAmount, payment.getAmount()));
        payment.setNotes("Stripe refund webhook processed");

        if (delta > 0 && Boolean.TRUE.equals(payment.getAppliedToInvoice())) {
            Long userId = payment.getUser() != null ? payment.getUser().getId() : null;
            if (userId == null) {
                throw new SecurityException("Refund webhook payment has no owner");
            }
            invoiceService.subtractPaymentForUser(
                    payment.getInvoice().getId(),
                    userId,
                    delta,
                    PaymentMode.STRIPE,
                    LocalDateTime.now()
            );
        }

        return paymentRepository.save(payment);
    }

    private void validateWebhookOwnership(PaymentIntent intent, Payment payment) {
        Long paymentUserId = payment.getUser() != null ? payment.getUser().getId() : null;
        String metadataUserId = intent.getMetadata() != null ? intent.getMetadata().get("userId") : null;
        String metadataInvoiceId = intent.getMetadata() != null ? intent.getMetadata().get("invoiceId") : null;

        if (paymentUserId == null
                || metadataUserId == null
                || !metadataUserId.equals(paymentUserId.toString())
                || payment.getInvoice() == null
                || metadataInvoiceId == null
                || !metadataInvoiceId.equals(payment.getInvoice().getId().toString())) {
            throw new SecurityException("Stripe webhook metadata does not match stored payment ownership");
        }
    }

    @Transactional
    private Payment verifyPayment(String paymentIntentId, Payment payment, Long userId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        return applyVerifiedIntent(intent, payment, userId);
    }

    private Payment applyVerifiedIntent(PaymentIntent intent, Payment payment, Long userId) {
        payment.setStripeStatus(intent.getStatus());
        payment.setStripeChargeId(intent.getLatestCharge());

        if ("succeeded".equals(intent.getStatus()) && !Boolean.TRUE.equals(payment.getAppliedToInvoice())) {
            Invoice invoice = payment.getInvoice();
            invoiceService.addPaymentForUser(invoice.getId(), userId, payment.getAmount(), PaymentMode.STRIPE, LocalDateTime.now());
            payment.setAppliedToInvoice(true);
            payment.setNotes("Stripe payment verified");
        } else if ("succeeded".equals(intent.getStatus())) {
            payment.setNotes("Stripe payment already verified");
        }

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPaymentHistory(Pageable pageable) {
        return paymentRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(
                securityUtils.getCurrentUserId(),
                pageable
        );
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Payment refundPayment(UUID paymentId, RefundRequest request) throws StripeException {
        Payment payment = paymentRepository.findByPaymentIdAndUserIdAndIsDeletedFalse(paymentId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        RefundCreateParams.Builder params = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId());

        double refundableAmount = payment.getAmount() - (payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0);
        double requestedAmount = request.getAmount() != null ? request.getAmount() : refundableAmount;

        if (requestedAmount <= 0 || requestedAmount > refundableAmount) {
            throw new IllegalArgumentException("Refund amount must be greater than zero and cannot exceed refundable amount");
        }

        if (request.getAmount() != null && request.getAmount() > 0) {
            params.setAmount(toMinorUnit(requestedAmount));
        }

        if (request.getReason() != null && !request.getReason().isBlank()) {
            params.putMetadata("reason", request.getReason());
        }

        Refund refund = Refund.create(params.build());

        payment.setStripeRefundId(refund.getId());
        payment.setStripeStatus(refund.getStatus());
        payment.setRefundedAmount((payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0) + requestedAmount);
        payment.setNotes("Refund requested" + (request.getReason() != null ? ": " + request.getReason() : ""));

        if ("succeeded".equals(refund.getStatus()) && Boolean.TRUE.equals(payment.getAppliedToInvoice())) {
            invoiceService.subtractPaymentForUser(
                    payment.getInvoice().getId(),
                    securityUtils.getCurrentUserId(),
                    requestedAmount,
                    PaymentMode.STRIPE,
                    LocalDateTime.now()
            );
        }

        return paymentRepository.save(payment);
    }

    private double remainingAmount(Invoice invoice) {
        if (invoice.getRemainingAmount() != null) return invoice.getRemainingAmount();
        if (invoice.getPendingAmount() != null) return invoice.getPendingAmount();
        return (invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0)
                - (invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0);
    }

    private long toMinorUnit(double amount) {
        return Math.round(amount * 100);
    }

    private double fromMinorUnit(Long amountMinor) {
        if (amountMinor == null) {
            return 0.0;
        }
        return amountMinor / 100.0;
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null || currency.isBlank() ? defaultCurrency : currency;
        return value.toLowerCase(Locale.ROOT);
    }
}
