package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ExpenseDTO;
import com.mybill.MyBill_Backend.entity.Expense;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ExpenseRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ExpenseService expenseService;

    private User mockUser;
    private Expense mockExpense;
    private UUID expenseId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        expenseId = UUID.randomUUID();

        mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        mockExpense = Expense.builder()
                .id(expenseId)
                .user(mockUser)
                .description("Software Subscription")
                .amount(BigDecimal.valueOf(99.99))
                .category("SOFTWARE")
                .expenseDate(LocalDate.now())
                .isDeleted(false)
                .version(1)
                .build();
    }

    @Test
    void getExpensesForUserSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findByUserIdAndIsDeletedFalseOrderByExpenseDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockExpense)));

        Page<Expense> expenses = expenseService.getExpensesForUser(Pageable.unpaged());

        assertThat(expenses.getContent()).hasSize(1);
        assertThat(expenses.getContent().get(0).getDescription()).isEqualTo("Software Subscription");
        verify(expenseRepository, times(1)).findByUserIdAndIsDeletedFalseOrderByExpenseDateDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void getExpenseByIdSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(mockExpense));

        Expense result = expenseService.getExpenseById(expenseId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expenseId);
        verify(expenseRepository, times(1)).findById(expenseId);
    }

    @Test
    void getExpenseByIdNotFoundThrowsException() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(expenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expense record not found");
    }

    @Test
    void getExpenseByIdAccessDeniedThrowsException() {
        User anotherUser = User.builder().id(2L).build();
        mockExpense.setUser(anotherUser);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(mockExpense));

        assertThatThrownBy(() -> expenseService.getExpenseById(expenseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied or record deleted");
    }

    @Test
    void createExpenseSuccess() {
        ExpenseDTO dto = ExpenseDTO.builder()
                .description("Office Desk")
                .amount(BigDecimal.valueOf(250.00))
                .category("OFFICE")
                .expenseDate(LocalDate.now())
                .taxAmount(BigDecimal.valueOf(18.00))
                .isRecurring(false)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense created = expenseService.createExpense(dto);

        assertThat(created).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Office Desk");
        assertThat(created.getAmount()).isEqualTo(BigDecimal.valueOf(250.00));
        assertThat(created.getUser().getId()).isEqualTo(1L);
        assertThat(created.getIsDeleted()).isFalse();
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    void updateExpenseSuccess() {
        ExpenseDTO updateDto = ExpenseDTO.builder()
                .description("Updated Desk")
                .amount(BigDecimal.valueOf(270.00))
                .category("OFFICE")
                .expenseDate(LocalDate.now())
                .isRecurring(true)
                .recurringCycle("MONTHLY")
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(mockExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense updated = expenseService.updateExpense(expenseId, updateDto);

        assertThat(updated).isNotNull();
        assertThat(updated.getDescription()).isEqualTo("Updated Desk");
        assertThat(updated.getAmount()).isEqualTo(BigDecimal.valueOf(270.00));
        assertThat(updated.getIsRecurring()).isTrue();
        assertThat(updated.getRecurringCycle()).isEqualTo("MONTHLY");
        assertThat(updated.getVersion()).isEqualTo(2);
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    void deleteExpenseSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(mockExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.deleteExpense(expenseId);

        assertThat(mockExpense.getIsDeleted()).isTrue();
        assertThat(mockExpense.getDeletedAt()).isNotNull();
        verify(expenseRepository, times(1)).save(mockExpense);
    }
}
