package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Quotation;
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

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    Optional<Quotation> findByIdAndUserId(UUID id, Long userId);

    Optional<Quotation> findByQuotationNumberAndUserId(String quotationNumber, Long userId);

    Page<Quotation> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client"})
    Page<Quotation> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client"})
    @Query("""
           SELECT q FROM Quotation q
           WHERE q.user.id = :userId
             AND (q.updatedAt > :lastTime OR (q.updatedAt = :lastTime AND q.id > :lastId))
           """)
    Page<Quotation> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client"})
    @Query("SELECT q FROM Quotation q WHERE q.user.id = :userId AND q.updatedAt >= :since")
    Page<Quotation> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    @Query(value = """
           SELECT q.*
           FROM public.quotation q
           LEFT JOIN public.clients c ON c.id = q.client_id
           WHERE q.user_id = :userId
             AND COALESCE(q.is_deleted, false) = false
             AND (
                :safeQuery = ''
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :safeQuery, '%'))
                OR LOWER(COALESCE(q.quotation_number, '')) LIKE LOWER(CONCAT('%', :safeQuery, '%'))
             )
           ORDER BY q.created_at DESC
           """,
           countQuery = """
           SELECT COUNT(*)
           FROM public.quotation q
           LEFT JOIN public.clients c ON c.id = q.client_id
           WHERE q.user_id = :userId
             AND COALESCE(q.is_deleted, false) = false
             AND (
                :safeQuery = ''
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :safeQuery, '%'))
                OR LOWER(COALESCE(q.quotation_number, '')) LIKE LOWER(CONCAT('%', :safeQuery, '%'))
             )
           """,
           nativeQuery = true)
    Page<Quotation> searchQuotations(
            @Param("userId") Long userId,
            @Param("safeQuery") String safeQuery,
            Pageable pageable
    );
}
