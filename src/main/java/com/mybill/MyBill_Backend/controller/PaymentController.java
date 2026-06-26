package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.PaymentIntentRequest;
import com.mybill.MyBill_Backend.dto.PaymentIntentResponse;
import com.mybill.MyBill_Backend.dto.PaymentSheetRequest;
import com.mybill.MyBill_Backend.dto.PaymentSheetResponse;
import com.mybill.MyBill_Backend.dto.PaymentVerificationRequest;
import com.mybill.MyBill_Backend.dto.RefundRequest;
import com.mybill.MyBill_Backend.entity.Payment;
import com.mybill.MyBill_Backend.service.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request
    ) throws StripeException {
        return ResponseEntity.ok(paymentService.createPaymentIntent(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<Payment> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request
    ) throws StripeException {
        return ResponseEntity.ok(paymentService.verifyPaymentForCurrentUser(request.getPaymentIntentId()));
    }

    @PostMapping("/stripe/payment-sheet")
    public ResponseEntity<PaymentSheetResponse> createPaymentSheet(
            @Valid @RequestBody PaymentSheetRequest request
    ) throws StripeException {
        return ResponseEntity.ok(paymentService.createPaymentSheet(request));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<Payment>> getPaymentHistory(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(pageable));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request
    ) throws StripeException {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request));
    }
}
