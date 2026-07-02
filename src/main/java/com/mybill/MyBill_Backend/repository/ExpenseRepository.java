package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByUserIdAndIsDeletedFalseOrderByExpenseDateDesc(Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "  AND e.isDeleted = false " +
           "  AND e.expenseDate >= :start AND e.expenseDate <= :end")
    double sumExpensesForPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
