package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.dto.InvoiceProjection;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.PaymentStatus;
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

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndUserId(UUID id, Long userId);

    Optional<Invoice> findByInvoiceNumberAndUserId(String invoiceNumber, Long userId);

    List<Invoice> findByClientIdAndUserIdAndIsDeletedFalse(UUID clientId, Long userId);

    Page<InvoiceProjection> findProjectedByClientIdAndUserIdAndIsDeletedFalse(UUID clientId, Long userId, Pageable pageable);

    Page<InvoiceProjection> findProjectedByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<Invoice> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    List<Invoice> findTop50ByIsDeletedFalseAndPaymentStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
            PaymentStatus paymentStatus,
            LocalDateTime dueDate
    );

    long countByUserIdAndIsDeletedFalse(Long userId);

    @Query("""
           SELECT SUM(i.totalAmount)
           FROM Invoice i
           WHERE i.user.id = :userId
             AND i.isDeleted = false
           """)
    Double getTotalRevenueByUserId(@Param("userId") Long userId);

    @Query("""
           SELECT SUM(i.totalAmount)
           FROM Invoice i
           WHERE i.user.id = :userId
             AND i.isDeleted = false
             AND EXTRACT(MONTH FROM i.createdDate) = :month
             AND EXTRACT(YEAR FROM i.createdDate) = :year
           """)
    Double getMonthlyRevenueByUserId(
            @Param("month") int month,
            @Param("year") int year,
            @Param("userId") Long userId
    );

    @Query(value = """
           SELECT i.*
           FROM public.invoice i
           LEFT JOIN public.clients c ON c.id = i.client_id
           WHERE i.user_id = :userId
             AND COALESCE(i.is_deleted, false) = false
             AND (:month IS NULL OR EXTRACT(MONTH FROM i.created_date) = :month)
             AND (:year IS NULL OR EXTRACT(YEAR FROM i.created_date) = :year)
             AND (
                :clientName = ''
                OR to_tsvector(
                    'simple',
                    COALESCE(i.invoice_number, '') || ' ' ||
                    COALESCE(i.notes, '') || ' ' ||
                    COALESCE(c.name, '') || ' ' ||
                    COALESCE(c.phone, '')
                ) @@ plainto_tsquery('simple', :clientName)
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :clientName, '%'))
                OR LOWER(COALESCE(i.invoice_number, '')) LIKE LOWER(CONCAT('%', :clientName, '%'))
             )
           ORDER BY
             CASE
               WHEN :clientName = '' THEN 0
               ELSE ts_rank_cd(
                    to_tsvector(
                        'simple',
                        COALESCE(i.invoice_number, '') || ' ' ||
                        COALESCE(i.notes, '') || ' ' ||
                        COALESCE(c.name, '') || ' ' ||
                        COALESCE(c.phone, '')
                    ),
                    plainto_tsquery('simple', :clientName)
               )
             END DESC,
             i.created_date DESC
           """,
           countQuery = """
           SELECT COUNT(*)
           FROM public.invoice i
           LEFT JOIN public.clients c ON c.id = i.client_id
           WHERE i.user_id = :userId
             AND COALESCE(i.is_deleted, false) = false
             AND (:month IS NULL OR EXTRACT(MONTH FROM i.created_date) = :month)
             AND (:year IS NULL OR EXTRACT(YEAR FROM i.created_date) = :year)
             AND (
                :clientName = ''
                OR to_tsvector(
                    'simple',
                    COALESCE(i.invoice_number, '') || ' ' ||
                    COALESCE(i.notes, '') || ' ' ||
                    COALESCE(c.name, '') || ' ' ||
                    COALESCE(c.phone, '')
                ) @@ plainto_tsquery('simple', :clientName)
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :clientName, '%'))
                OR LOWER(COALESCE(i.invoice_number, '')) LIKE LOWER(CONCAT('%', :clientName, '%'))
             )
           """,
           nativeQuery = true)
    Page<Invoice> searchInvoices(
            @Param("userId") Long userId,
            @Param("clientName") String clientName,
            @Param("month") Integer month,
            @Param("year") Integer year,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT i
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientName = '' OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :clientName, '%')))
                      AND (:month IS NULL OR EXTRACT(MONTH FROM i.createdDate) = :month)
                      AND (:year IS NULL OR EXTRACT(YEAR FROM i.createdDate) = :year)
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientName = '' OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :clientName, '%')))
                      AND (:month IS NULL OR EXTRACT(MONTH FROM i.createdDate) = :month)
                      AND (:year IS NULL OR EXTRACT(YEAR FROM i.createdDate) = :year)
                    """
    )
    Page<InvoiceProjection> searchProjectedInvoices(
            @Param("userId") Long userId,
            @Param("clientName") String clientName,
            @Param("month") Integer month,
            @Param("year") Integer year,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT i
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientId IS NULL OR i.client.id = :clientId)
                      AND (:startDate IS NULL OR i.createdDate >= :startDate)
                      AND (:endDate IS NULL OR i.createdDate <= :endDate)
                      AND (:minAmount IS NULL OR i.totalAmount >= :minAmount)
                      AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount)
                      AND (:statuses IS NULL OR i.paymentStatus IN :statuses)
                      AND (
                        :query = ''
                        OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientId IS NULL OR i.client.id = :clientId)
                      AND (:startDate IS NULL OR i.createdDate >= :startDate)
                      AND (:endDate IS NULL OR i.createdDate <= :endDate)
                      AND (:minAmount IS NULL OR i.totalAmount >= :minAmount)
                      AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount)
                      AND (:statuses IS NULL OR i.paymentStatus IN :statuses)
                      AND (
                        :query = ''
                        OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """
    )
    Page<InvoiceProjection> filterInvoices(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT i
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientId IS NULL OR i.client.id = :clientId)
                      AND (:startDate IS NULL OR i.createdDate >= :startDate)
                      AND (:endDate IS NULL OR i.createdDate <= :endDate)
                      AND (:minAmount IS NULL OR i.totalAmount >= :minAmount)
                      AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount)
                      AND (
                        :query = ''
                        OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Invoice i
                    WHERE i.user.id = :userId
                      AND i.isDeleted = false
                      AND (:clientId IS NULL OR i.client.id = :clientId)
                      AND (:startDate IS NULL OR i.createdDate >= :startDate)
                      AND (:endDate IS NULL OR i.createdDate <= :endDate)
                      AND (:minAmount IS NULL OR i.totalAmount >= :minAmount)
                      AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount)
                      AND (
                        :query = ''
                        OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """
    )
    Page<InvoiceProjection> filterInvoicesWithoutStatuses(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            Pageable pageable
    );

    @Query("""
           SELECT SUM(i.totalAmount)
           FROM Invoice i
           WHERE i.user.id = :userId
             AND i.isDeleted = false
             AND EXTRACT(YEAR FROM i.createdDate) = :year
             AND EXTRACT(MONTH FROM i.createdDate) = :month
           """)
    Double sumTotalAmountByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    List<Invoice> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<Invoice> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"client"})
    Page<Invoice> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"client"})
    Page<Invoice> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "user"})
    List<Invoice> findByIsDeletedFalseAndPaymentStatusIn(List<PaymentStatus> statuses);

    @EntityGraph(attributePaths = {"client", "user"})
    Page<Invoice> findByIsDeletedFalseAndPaymentStatusIn(List<PaymentStatus> statuses, Pageable pageable);

    @Query("""
           SELECT 
             COALESCE(SUM(COALESCE(i.grossAmount, COALESCE(i.subtotal, COALESCE(i.totalAmount, 0.0)))), 0.0),
             COALESCE(SUM(COALESCE(i.paidAmount, 0.0)), 0.0),
             COALESCE(SUM(COALESCE(i.pendingAmount, COALESCE(i.remainingAmount, 0.0))), 0.0)
           FROM Invoice i
           WHERE i.client.id = :clientId
             AND i.user.id = :userId
             AND i.isDeleted = false
           """)
    List<Object[]> getClientFinancialStats(
            @Param("clientId") java.util.UUID clientId,
            @Param("userId") Long userId
    );

    @Query("""
           SELECT i FROM Invoice i
           WHERE i.client.id = :clientId
             AND i.user.id = :userId
             AND i.isDeleted = false
             AND COALESCE(i.pendingAmount, COALESCE(i.remainingAmount, 0.0)) > 0
           ORDER BY COALESCE(i.dueDate, COALESCE(i.invoiceDate, COALESCE(i.createdDate, '9999-12-31 23:59:59'))) ASC
           """)
    List<Invoice> findPendingInvoicesByClient(
            @Param("clientId") java.util.UUID clientId,
            @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {"client"})
    @Query("""
           SELECT i FROM Invoice i
           WHERE i.user.id = :userId
             AND (i.updatedAt > :lastTime OR (i.updatedAt = :lastTime AND i.id > :lastId))
           """)
    Page<Invoice> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client"})
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId AND i.updatedAt >= :since")
    Page<Invoice> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
