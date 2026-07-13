package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.ClientLedgerEntry;
import com.mybill.MyBill_Backend.entity.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientLedgerEntryRepository extends JpaRepository<ClientLedgerEntry, UUID> {

    Optional<ClientLedgerEntry> findByIdAndUserId(UUID id, Long userId);

    Page<ClientLedgerEntry> findByClientIdAndUserIdAndIsDeletedFalseOrderByTransactionDateDesc(
            UUID clientId,
            Long userId,
            Pageable pageable
    );

    List<ClientLedgerEntry> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<ClientLedgerEntry> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"client", "invoice", "payment"})
    Page<ClientLedgerEntry> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "invoice", "payment"})
    Page<ClientLedgerEntry> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAt, Pageable pageable);

    boolean existsByInvoiceIdAndTypeAndUserIdAndIsDeletedFalse(UUID invoiceId, LedgerEntryType type, Long userId);

    @Query("""
           SELECT COALESCE(SUM(
               CASE
                   WHEN le.type = 'ADVANCE_RECEIVED' THEN le.amount
                   WHEN le.type = 'ADVANCE_APPLIED' THEN -le.amount
                   WHEN le.type = 'ADJUSTMENT' AND le.direction = 'CREDIT' THEN le.amount
                   WHEN le.type = 'ADJUSTMENT' AND le.direction = 'DEBIT' THEN -le.amount
                   ELSE 0
               END
           ), 0.0)
           FROM ClientLedgerEntry le
           WHERE le.client.id = :clientId
             AND le.user.id = :userId
             AND le.isDeleted = false
           """)
    Double getAdvanceBalance(@Param("clientId") UUID clientId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"client", "invoice", "payment"})
    @Query("""
           SELECT l FROM ClientLedgerEntry l
           WHERE l.user.id = :userId
             AND (l.updatedAt > :lastTime OR (l.updatedAt = :lastTime AND l.id > :lastId))
           """)
    Page<ClientLedgerEntry> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "invoice", "payment"})
    @Query("SELECT l FROM ClientLedgerEntry l WHERE l.user.id = :userId AND l.updatedAt >= :since")
    Page<ClientLedgerEntry> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
