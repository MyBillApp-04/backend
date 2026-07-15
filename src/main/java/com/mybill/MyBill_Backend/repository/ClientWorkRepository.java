package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.dto.ClientSummaryProjection;
import com.mybill.MyBill_Backend.dto.ClientWorkProjection;
import com.mybill.MyBill_Backend.entity.ClientWork;
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

public interface ClientWorkRepository extends JpaRepository<ClientWork, UUID> {

    List<ClientWork> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<ClientWork> findByIdAndUserId(UUID id, Long userId);

    List<ClientWork> findByClientIdAndUserIdAndIsDeletedFalse(UUID clientId, Long userId);

    Page<ClientWork> findByClientIdAndUserIdAndIsDeletedFalse(UUID clientId, Long userId, Pageable pageable);

    List<ClientWork> findByClientIdAndBilledFalseAndUserIdAndIsDeletedFalse(UUID clientId, Long userId);

    Page<ClientWork> findByClientIdAndBilledFalseAndUserIdAndIsDeletedFalse(UUID clientId, Long userId, Pageable pageable);

    List<ClientWork> findByInvoiceIdAndUserId(UUID invoiceId, Long userId);

    @Query("""
           SELECT w FROM ClientWork w
           WHERE w.user.id = :userId
             AND w.isDeleted = false
           ORDER BY w.date DESC, w.createdAt DESC
           """)
    List<ClientWork> findAllByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("""
           SELECT w FROM ClientWork w
           WHERE w.user.id = :userId
             AND w.isDeleted = false
           ORDER BY w.date DESC, w.createdAt DESC
           """)
    Page<ClientWork> findAllByUserIdOrderByDateDesc(@Param("userId") Long userId, Pageable pageable);

    long countByBilledFalseAndUserIdAndIsDeletedFalse(Long userId);

    @Query("""
           SELECT SUM(w.amount)
           FROM ClientWork w
           WHERE w.client.id = :clientId
             AND w.user.id = :userId
             AND w.isDeleted = false
           """)
    Double getTotalAmountByClientAndUserId(
            @Param("clientId") UUID clientId,
            @Param("userId") Long userId
    );

    @Query("""
           SELECT
               c.id AS clientId,
               c.name AS clientName,
               COUNT(w.id) AS totalWorks,
               COALESCE(SUM(w.amount), 0.0) AS totalAmount
           FROM Client c
           LEFT JOIN ClientWork w
                ON w.client.id = c.id
               AND w.user.id = :userId
               AND w.isDeleted = false
           WHERE c.user.id = :userId
             AND c.isDeleted = false
           GROUP BY c.id, c.name
           """)
    List<ClientSummaryProjection> getClientSummaryForUser(@Param("userId") Long userId);

    @Query("""
           SELECT COALESCE(SUM(w.amount), 0.0)
           FROM ClientWork w
           WHERE w.user.id = :userId
             AND w.isDeleted = false
             AND w.date >= :startOfDay
             AND w.date < :startOfNextDay
           """)
    Double getTodayEarningsForUser(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );

    List<ClientWork> findTop5ByUserIdAndIsDeletedFalseOrderByIdDesc(Long userId);

    List<ClientWork> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<ClientWork> findByClientIdAndUserIdAndBilledFalseAndIsDeletedFalseOrderByDateAscCreatedAtAsc(
            UUID clientId,
            Long userId
    );

    List<ClientWork> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"client", "invoice"})
    Page<ClientWork> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "invoice"})
    Page<ClientWork> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    @Query("""
           SELECT w
           FROM ClientWork w
           LEFT JOIN w.client c
           WHERE w.user.id = :userId
             AND w.isDeleted = false
             AND (
                LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           """)
    Page<ClientWork> searchByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );

    // NEW: Dashboard optimized projection query that completely avoids Entity N+1 fetching
    @Query("""
           SELECT
               w.id AS id,
               c.id AS clientId,
               c.name AS clientName,
               w.description AS description,
               w.rate AS rate,
               w.quantity AS quantity,
               w.amount AS amount,
               w.billed AS billed,
               w.isDeleted AS isDeleted,
               i.id AS invoiceId,
               w.date AS workDate,
               w.createdAt AS createdAt,
               w.updatedAt AS updatedAt,
               w.deletedAt AS deletedAt
           FROM ClientWork w
           LEFT JOIN w.client c
           LEFT JOIN w.invoice i
           WHERE w.user.id = :userId
             AND w.isDeleted = false
           ORDER BY w.id DESC
           """)
    List<ClientWorkProjection> findRecentActivityProjected(@Param("userId") Long userId, Pageable pageable);
    @EntityGraph(attributePaths = {"client", "invoice"})
    @Query("""
           SELECT w FROM ClientWork w
           WHERE w.user.id = :userId
             AND (w.updatedAt > :lastTime OR (w.updatedAt = :lastTime AND w.id > :lastId))
           """)
    Page<ClientWork> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "invoice"})
    @Query("SELECT w FROM ClientWork w WHERE w.user.id = :userId AND w.updatedAt >= :since")
    Page<ClientWork> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
