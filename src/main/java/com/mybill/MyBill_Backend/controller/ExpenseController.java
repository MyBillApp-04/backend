package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ExpenseDTO;
import com.mybill.MyBill_Backend.entity.Expense;
import com.mybill.MyBill_Backend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getExpenses() {
        List<ExpenseDTO> dtos = expenseService.getExpensesForUser()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpense(@PathVariable UUID id) {
        Expense expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(toDTO(expense));
    }

    @PostMapping
    public ResponseEntity<ExpenseDTO> createExpense(@RequestBody ExpenseDTO dto) {
        Expense created = expenseService.createExpense(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable UUID id,
            @RequestBody ExpenseDTO dto
    ) {
        Expense updated = expenseService.updateExpense(id, dto);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    private ExpenseDTO toDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .vendorName(expense.getVendorName())
                .taxAmount(expense.getTaxAmount())
                .receiptUrl(expense.getReceiptUrl())
                .isRecurring(expense.getIsRecurring())
                .recurringCycle(expense.getRecurringCycle())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .isDeleted(expense.getIsDeleted())
                .deletedAt(expense.getDeletedAt())
                .version(expense.getVersion())
                .build();
    }
}
