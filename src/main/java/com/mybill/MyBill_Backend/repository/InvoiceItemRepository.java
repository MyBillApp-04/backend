package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.InvoiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

    Page<InvoiceItem> findByUserId(Long userId, Pageable pageable);

    Page<InvoiceItem> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );
}
