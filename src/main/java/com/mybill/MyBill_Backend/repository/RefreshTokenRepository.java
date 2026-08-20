package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.RefreshToken;
import com.mybill.MyBill_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user = :user AND rt.revokedAt IS NULL")
    void revokeAllByUser(@Param("user") User user, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    Optional<RefreshToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<RefreshToken> findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(Long userId, String deviceId);

    Optional<RefreshToken> findFirstByUserIdAndDeviceIdAndTrustedTrueOrderByCreatedAtDesc(Long userId, String deviceId);
}
