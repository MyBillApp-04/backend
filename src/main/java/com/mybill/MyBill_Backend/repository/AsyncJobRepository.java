package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.AsyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    List<AsyncJob> findTop10ByStatusInAndNextRunAtLessThanEqualOrderByCreatedAtAsc(
            List<String> statuses, 
            LocalDateTime time
    );

    @Query("""
           SELECT j
           FROM AsyncJob j
           WHERE j.status IN :statuses
             AND j.nextRunAt <= :time
           ORDER BY j.createdAt ASC
           """)
    List<AsyncJob> findExecutableJobs(
            @Param("statuses") List<String> statuses,
            @Param("time") LocalDateTime time,
            Pageable pageable
    );

    long countByStatus(String status);

    @Query("SELECT j.status, COUNT(j) FROM AsyncJob j GROUP BY j.status")
    List<Object[]> getStatusCounts();

    Page<AsyncJob> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
           UPDATE AsyncJob j
           SET j.status = 'FAILED',
               j.lastError = :reason,
               j.nextRunAt = :now,
               j.updatedAt = :now
           WHERE j.status = 'RUNNING'
             AND j.updatedAt < :cutoff
           """)
    int recoverStaleRunningJobs(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("now") LocalDateTime now,
            @Param("reason") String reason
    );
}
