package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.SyncDeviceState;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SyncDeviceStateRepository extends JpaRepository<SyncDeviceState, UUID> {
    Optional<SyncDeviceState> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Transactional
    @Query("""
            update SyncDeviceState state
            set state.lastPulledAt = :serverTime,
                state.lastPushedAt = case when :pushed = true then :serverTime else state.lastPushedAt end,
                state.lastSeenAt = :serverTime,
                state.conflictCount = coalesce(state.conflictCount, 0) + :conflictCount
            where state.user.id = :userId and state.deviceId = :deviceId
            """)
    int updateSyncState(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("serverTime") LocalDateTime serverTime,
            @Param("pushed") boolean pushed,
            @Param("conflictCount") int conflictCount
    );
}
