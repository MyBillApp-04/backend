package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.dto.DashboardStatsProjection;
import com.mybill.MyBill_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DashboardRepository extends JpaRepository<User, Long> {

    @Query(value = """
            WITH top_client_cte AS (
                SELECT c.id AS client_id, c.name AS client_name
                FROM invoice inv
                JOIN clients c ON inv.client_id = c.id
                WHERE inv.user_id = :userId
                AND COALESCE(inv.is_deleted, false) = false
                GROUP BY c.id, c.name
                ORDER BY SUM(inv.total_amount) DESC
                LIMIT 1
            )
            SELECT
                (
                    SELECT COUNT(*)
                    FROM clients c
                    WHERE c.user_id = :userId
                    AND COALESCE(c.is_deleted, false) = false
                ) AS totalClients,

                COALESCE(SUM(CASE WHEN i.created_date >= :monthStart AND i.created_date < :nextMonthStart THEN i.total_amount ELSE 0 END), 0) AS thisMonthBilled,

                COALESCE(SUM(CASE WHEN i.payment_date >= :monthStart AND i.payment_date < :nextMonthStart THEN i.paid_amount ELSE 0 END), 0) AS thisMonthReceived,

                COALESCE(SUM(i.pending_amount), 0) AS totalPending,

                COUNT(CASE WHEN i.payment_status IN ('UNPAID', 'PARTIALLY_PAID') THEN 1 END) AS pendingInvoices,

                (SELECT client_name FROM top_client_cte) AS topClient,

                (SELECT CAST(client_id AS VARCHAR) FROM top_client_cte) AS topClientId
            FROM invoice i
            WHERE i.user_id = :userId
            AND COALESCE(i.is_deleted, false) = false
            """, nativeQuery = true)
    DashboardStatsProjection getDashboardStats(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("nextMonthStart") LocalDateTime nextMonthStart
    );
}