package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ExpenseDTO;
import com.mybill.MyBill_Backend.entity.Expense;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ExpenseRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<Expense> getExpensesForUser() {
        Long userId = securityUtils.getCurrentUserId();
        return expenseRepository.findByUserIdAndIsDeletedFalseOrderByExpenseDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public Expense getExpenseById(UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense record not found"));
        if (!expense.getUser().getId().equals(userId) || Boolean.TRUE.equals(expense.getIsDeleted())) {
            throw new RuntimeException("Access denied or record deleted");
        }
        return expense;
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Expense createExpense(ExpenseDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        Expense expense = Expense.builder()
                .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
                .user(user)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .category(dto.getCategory())
                .expenseDate(dto.getExpenseDate())
                .vendorName(dto.getVendorName())
                .taxAmount(dto.getTaxAmount())
                .receiptUrl(dto.getReceiptUrl())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurringCycle(dto.getRecurringCycle())
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .version(1)
                .build();

        return expenseRepository.save(expense);
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Expense updateExpense(UUID id, ExpenseDTO dto) {
        Expense expense = getExpenseById(id);

        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setVendorName(dto.getVendorName());
        expense.setTaxAmount(dto.getTaxAmount());
        expense.setReceiptUrl(dto.getReceiptUrl());
        expense.setIsRecurring(dto.getIsRecurring());
        expense.setRecurringCycle(dto.getRecurringCycle());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setVersion(expense.getVersion() + 1);

        return expenseRepository.save(expense);
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteExpense(UUID id) {
        Expense expense = getExpenseById(id);
        expense.setIsDeleted(true);
        expense.setDeletedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setVersion(expense.getVersion() + 1);
        expenseRepository.save(expense);
    }
}
