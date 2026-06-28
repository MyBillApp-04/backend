package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.Payment;
import com.mybill.MyBill_Backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/history")
    public ResponseEntity<Page<Payment>> getPaymentHistory(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(pageable));
    }
}
