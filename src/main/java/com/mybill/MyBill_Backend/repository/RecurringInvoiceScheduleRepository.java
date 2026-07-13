package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringInvoiceScheduleRepository extends JpaRepository<RecurringInvoiceSchedule, UUID> {

    List<RecurringInvoiceSchedule> findByUserIdAndIsDeletedFalse(Long userId);

    List<RecurringInvoiceSchedule> findByStatusAndNextRunDateBeforeAndIsDeletedFalse(String status, LocalDateTime date);

    List<RecurringInvoiceSchedule> findByIdIn(Collection<UUID> ids);

    @Query("""
            select s.id
            from RecurringInvoiceSchedule s
            where s.status = :status
              and s.nextRunDate < :date
              and s.isDeleted = false
            order by s.nextRunDate asc, s.id asc
            """)
    List<UUID> findDueIds(@Param("status") String status, @Param("date") LocalDateTime date);
}
