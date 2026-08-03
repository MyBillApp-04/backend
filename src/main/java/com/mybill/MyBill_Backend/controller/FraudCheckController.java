package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.FraudCheck;
import com.mybill.MyBill_Backend.repository.FraudCheckRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fraud-checks")
@RequiredArgsConstructor
public class FraudCheckController {

    private final FraudCheckRepository fraudCheckRepository;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Page<FraudCheck>> getFraudChecks(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(fraudCheckRepository.findByUserId(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudCheck> getFraudCheck(@PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        FraudCheck check = fraudCheckRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Fraud check report not found"));
        return ResponseEntity.ok(check);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<FraudCheck> getFraudCheckByPayment(@PathVariable UUID paymentId) {
        Long userId = securityUtils.getCurrentUserId();
        FraudCheck check = fraudCheckRepository.findByPaymentPaymentIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new NotFoundException("Fraud check report not found for this payment"));
        return ResponseEntity.ok(check);
    }
}
