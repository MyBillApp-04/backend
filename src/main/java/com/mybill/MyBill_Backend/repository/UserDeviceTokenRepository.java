package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    List<UserDeviceToken> findByUserId(Long userId);

    Optional<UserDeviceToken> findByUserIdAndFcmToken(Long userId, String fcmToken);

    void deleteByFcmToken(String fcmToken);

    void deleteByUserIdAndFcmToken(Long userId, String fcmToken);
}
