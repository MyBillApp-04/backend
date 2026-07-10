package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.AsyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    List<AsyncJob> findTop10ByStatusInAndNextRunAtLessThanEqualOrderByCreatedAtAsc(
            List<String> statuses, 
            LocalDateTime time
    );

    long countByStatus(String status);

    @Query("SELECT j.status, COUNT(j) FROM AsyncJob j GROUP BY j.status")
    List<Object[]> getStatusCounts();

    Page<AsyncJob> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
