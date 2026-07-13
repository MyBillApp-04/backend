package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.InvoiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<InvoiceItem> findByUserId(Long userId);

    List<InvoiceItem> findByInvoiceIdAndUserIdAndIsDeletedFalse(UUID invoiceId, Long userId);

    Optional<InvoiceItem> findTopByWorkIdAndUserIdAndIsDeletedFalseOrderByInvoiceInvoiceDateDescCreatedAtDesc(
            UUID workId,
            Long userId
    );

    boolean existsByWorkIdAndUserIdAndIsDeletedFalse(UUID workId, Long userId);

    Optional<InvoiceItem> findByIdAndUserId(UUID id, Long userId);

    @EntityGraph(attributePaths = {"invoice", "work"})
    Page<InvoiceItem> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"invoice", "work"})
    Page<InvoiceItem> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"invoice", "work"})
    @Query("""
           SELECT i FROM InvoiceItem i
           WHERE i.user.id = :userId
             AND (i.updatedAt > :lastTime OR (i.updatedAt = :lastTime AND i.id > :lastId))
           """)
    Page<InvoiceItem> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"invoice", "work"})
    @Query("SELECT i FROM InvoiceItem i WHERE i.user.id = :userId AND i.updatedAt >= :since")
    Page<InvoiceItem> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
