package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @EntityGraph(attributePaths = {"client", "invoice"})
    List<Payment> findByUserIdAndIsDeletedFalseOrderByDateDesc(Long userId);

    @EntityGraph(attributePaths = {"client", "invoice"})
    Page<Payment> findByUserIdAndIsDeletedFalseOrderByDateDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "invoice"})
    List<Payment> findByClientIdAndUserIdAndIsDeletedFalseOrderByDateDesc(UUID clientId, Long userId);

    Optional<Payment> findByPaymentIdAndUserIdAndIsDeletedFalse(UUID paymentId, Long userId);
    Optional<Payment> findByStripePaymentIntentIdAndIsDeletedFalse(String stripePaymentIntentId);
    Optional<Payment> findByStripePaymentIntentIdAndUserIdAndIsDeletedFalse(String stripePaymentIntentId, Long userId);
}
