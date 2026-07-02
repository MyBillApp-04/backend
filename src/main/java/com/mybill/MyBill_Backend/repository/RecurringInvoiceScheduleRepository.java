package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringInvoiceScheduleRepository extends JpaRepository<RecurringInvoiceSchedule, UUID> {

    List<RecurringInvoiceSchedule> findByUserIdAndIsDeletedFalse(Long userId);

    List<RecurringInvoiceSchedule> findByStatusAndNextRunDateBeforeAndIsDeletedFalse(String status, LocalDateTime date);
}
