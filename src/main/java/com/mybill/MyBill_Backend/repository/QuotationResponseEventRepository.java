package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.QuotationResponseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuotationResponseEventRepository extends JpaRepository<QuotationResponseEvent, UUID> {
    List<QuotationResponseEvent> findByQuotationIdOrderByRespondedAtDesc(UUID quotationId);
}
