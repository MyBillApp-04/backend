package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserIdAndIsDeletedFalseOrderByDateDesc(Long userId);
    Page<Payment> findByUserIdAndIsDeletedFalseOrderByDateDesc(Long userId, Pageable pageable);
    List<Payment> findByClientIdAndUserIdAndIsDeletedFalseOrderByDateDesc(UUID clientId, Long userId);
    Optional<Payment> findByPaymentIdAndUserIdAndIsDeletedFalse(UUID paymentId, Long userId);
    Optional<Payment> findByStripePaymentIntentIdAndIsDeletedFalse(String stripePaymentIntentId);
    Optional<Payment> findByStripePaymentIntentIdAndUserIdAndIsDeletedFalse(String stripePaymentIntentId, Long userId);
}
