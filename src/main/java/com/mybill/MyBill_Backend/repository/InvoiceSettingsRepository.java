package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceSettingsRepository extends JpaRepository<InvoiceSettings, UUID> {
    Optional<InvoiceSettings> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSettings s WHERE s.user.id = :userId")
    Optional<InvoiceSettings> findByUserIdForUpdate(@Param("userId") Long userId);
}
