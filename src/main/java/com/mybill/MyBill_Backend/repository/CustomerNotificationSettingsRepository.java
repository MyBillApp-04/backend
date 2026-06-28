package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.CustomerNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerNotificationSettingsRepository extends JpaRepository<CustomerNotificationSettings, UUID> {
    Optional<CustomerNotificationSettings> findByUserId(Long userId);
}
