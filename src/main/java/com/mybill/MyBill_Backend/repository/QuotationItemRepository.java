package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.QuotationItem;
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

public interface QuotationItemRepository extends JpaRepository<QuotationItem, UUID> {

    List<QuotationItem> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<QuotationItem> findByUserId(Long userId);

    List<QuotationItem> findByQuotationIdAndUserIdAndIsDeletedFalse(UUID quotationId, Long userId);

    Optional<QuotationItem> findByIdAndUserId(UUID id, Long userId);

    @EntityGraph(attributePaths = {"quotation"})
    Page<QuotationItem> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"quotation"})
    Page<QuotationItem> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"quotation"})
    @Query("""
           SELECT qi FROM QuotationItem qi
           WHERE qi.user.id = :userId
             AND (qi.updatedAt > :lastTime OR (qi.updatedAt = :lastTime AND qi.id > :lastId))
           """)
    Page<QuotationItem> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"quotation"})
    @Query("SELECT qi FROM QuotationItem qi WHERE qi.user.id = :userId AND qi.updatedAt >= :since")
    Page<QuotationItem> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
