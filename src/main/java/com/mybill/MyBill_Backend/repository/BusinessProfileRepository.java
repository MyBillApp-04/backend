package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    // FIX: look up by the owning user, not just the first row globally
    Optional<BusinessProfile> findByUserId(Long userId);
}