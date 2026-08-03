package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.FraudCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {
    Page<FraudCheck> findByUserId(Long userId, Pageable pageable);
    Optional<FraudCheck> findByPaymentPaymentIdAndUserId(UUID paymentId, Long userId);
    Optional<FraudCheck> findByIdAndUserId(UUID id, Long userId);
}
