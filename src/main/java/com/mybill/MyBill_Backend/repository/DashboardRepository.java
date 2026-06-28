package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.dto.DashboardStatsProjection;
import com.mybill.MyBill_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DashboardRepository extends JpaRepository<User, Long> {

    @Query(value = """
            SELECT
                (
                    SELECT COUNT(*)
                    FROM clients c
                    WHERE c.user_id = :userId
                    AND COALESCE(c.is_deleted, false) = false
                ) AS totalClients,

                (
                    SELECT COALESCE(SUM(i.total_amount), 0)
                    FROM invoice i
                    WHERE i.user_id = :userId
                    AND COALESCE(i.is_deleted, false) = false
                    AND i.created_date >= :monthStart
                    AND i.created_date < :nextMonthStart
                ) AS thisMonthBilled,

                (
                    SELECT COALESCE(SUM(i.paid_amount), 0)
                    FROM invoice i
                    WHERE i.user_id = :userId
                    AND COALESCE(i.is_deleted, false) = false
                    AND i.payment_date >= :monthStart
                    AND i.payment_date < :nextMonthStart
                ) AS thisMonthReceived,

                (
                    SELECT COALESCE(SUM(i.pending_amount), 0)
                    FROM invoice i
                    WHERE i.user_id = :userId
                    AND COALESCE(i.is_deleted, false) = false
                ) AS totalPending,

                (
                    SELECT COUNT(*)
                    FROM invoice i
                    WHERE i.user_id = :userId
                    AND COALESCE(i.is_deleted, false) = false
                    AND i.payment_status IN ('UNPAID', 'PARTIALLY_PAID')
                ) AS pendingInvoices,

                (
                    SELECT c.name
                    FROM invoice inv
                    JOIN clients c ON inv.client_id = c.id
                    WHERE inv.user_id = :userId
                    AND COALESCE(inv.is_deleted, false) = false
                    GROUP BY c.id, c.name
                    ORDER BY SUM(inv.total_amount) DESC
                    LIMIT 1
                ) AS topClient,

                (
                    SELECT CAST(c.id AS VARCHAR)
                    FROM invoice inv
                    JOIN clients c ON inv.client_id = c.id
                    WHERE inv.user_id = :userId
                    AND COALESCE(inv.is_deleted, false) = false
                    GROUP BY c.id, c.name
                    ORDER BY SUM(inv.total_amount) DESC
                    LIMIT 1
                ) AS topClientId
            """, nativeQuery = true)
    DashboardStatsProjection getDashboardStats(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("nextMonthStart") LocalDateTime nextMonthStart
    );
}