package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.RecurringInvoiceScheduleDTO;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.RecurringInvoiceScheduleRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecurringInvoiceScheduleServiceTest {

    @Mock
    private RecurringInvoiceScheduleRepository scheduleRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RecurringInvoiceSchedulerSelfProxy schedulerSelfProxy;

    @InjectMocks
    private RecurringInvoiceScheduleService scheduleService;

    private User mockUser;
    private Client mockClient;
    private RecurringInvoiceSchedule mockSchedule;
    private UUID scheduleId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduleId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        mockClient = Client.builder()
                .id(clientId)
                .name("Acme Corp")
                .user(mockUser)
                .isDeleted(false)
                .build();

        mockSchedule = RecurringInvoiceSchedule.builder()
                .id(scheduleId)
                .client(mockClient)
                .user(mockUser)
                .description("Monthly Retainer")
                .amount(BigDecimal.valueOf(500.00))
                .billingCycle("MONTHLY")
                .cronExpression("0 0 1 1 * ?")
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .nextRunDate(LocalDateTime.now().plusMonths(1))
                .autoCharge(false)
                .isDeleted(false)
                .version(1)
                .build();
    }

    @Test
    void getSchedulesForUserSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(Arrays.asList(mockSchedule));

        List<RecurringInvoiceSchedule> schedules = scheduleService.getSchedulesForUser();

        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0).getDescription()).isEqualTo("Monthly Retainer");
        verify(scheduleRepository, times(1)).findByUserIdAndIsDeletedFalse(1L);
    }

    @Test
    void getScheduleByIdSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));

        RecurringInvoiceSchedule result = scheduleService.getScheduleById(scheduleId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(scheduleId);
        verify(scheduleRepository, times(1)).findById(scheduleId);
    }

    @Test
    void getScheduleByIdNotFoundThrowsException() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getScheduleById(scheduleId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Schedule not found");
    }

    @Test
    void getScheduleByIdAccessDeniedThrowsException() {
        User anotherUser = User.builder().id(2L).build();
        mockSchedule.setUser(anotherUser);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));

        assertThatThrownBy(() -> scheduleService.getScheduleById(scheduleId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied or schedule deleted");
    }

    @Test
    void createScheduleSuccess() {
        RecurringInvoiceScheduleDTO dto = RecurringInvoiceScheduleDTO.builder()
                .clientId(clientId)
                .description("Weekly Cleaning")
                .amount(BigDecimal.valueOf(150.00))
                .billingCycle("WEEKLY")
                .cronExpression("0 0 12 ? * SUN")
                .startDate(LocalDate.now())
                .autoCharge(true)
                .build();

        LocalDateTime mockNextRun = LocalDateTime.now().plusWeeks(1);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, 1L)).thenReturn(Optional.of(mockClient));
        when(schedulerSelfProxy.calculateNextRunDate(eq("0 0 12 ? * SUN"), eq("WEEKLY"), any(LocalDateTime.class)))
                .thenReturn(mockNextRun);
        when(scheduleRepository.save(any(RecurringInvoiceSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringInvoiceSchedule created = scheduleService.createSchedule(dto);

        assertThat(created).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Weekly Cleaning");
        assertThat(created.getAmount()).isEqualTo(BigDecimal.valueOf(150.00));
        assertThat(created.getBillingCycle()).isEqualTo("WEEKLY");
        assertThat(created.getNextRunDate()).isEqualTo(mockNextRun);
        assertThat(created.getAutoCharge()).isTrue();
        verify(scheduleRepository, times(1)).save(any(RecurringInvoiceSchedule.class));
    }

    @Test
    void updateScheduleSuccess() {
        RecurringInvoiceScheduleDTO updateDto = RecurringInvoiceScheduleDTO.builder()
                .description("Updated Monthly retainer")
                .amount(BigDecimal.valueOf(550.00))
                .billingCycle("MONTHLY")
                .cronExpression("0 0 1 1 * ?")
                .autoCharge(false)
                .build();

        LocalDateTime mockNextRun = LocalDateTime.now().plusMonths(1);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(schedulerSelfProxy.calculateNextRunDate(eq("0 0 1 1 * ?"), eq("MONTHLY"), any(LocalDateTime.class)))
                .thenReturn(mockNextRun);
        when(scheduleRepository.save(any(RecurringInvoiceSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringInvoiceSchedule updated = scheduleService.updateSchedule(scheduleId, updateDto);

        assertThat(updated).isNotNull();
        assertThat(updated.getDescription()).isEqualTo("Updated Monthly retainer");
        assertThat(updated.getAmount()).isEqualTo(BigDecimal.valueOf(550.00));
        assertThat(updated.getVersion()).isEqualTo(2);
        verify(scheduleRepository, times(1)).save(any(RecurringInvoiceSchedule.class));
    }

    @Test
    void setScheduleStatusSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(scheduleRepository.save(any(RecurringInvoiceSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringInvoiceSchedule updated = scheduleService.setScheduleStatus(scheduleId, "PAUSED");

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("PAUSED");
        assertThat(updated.getVersion()).isEqualTo(2);
        verify(scheduleRepository, times(1)).save(updated);
    }

    @Test
    void deleteScheduleSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(scheduleRepository.save(any(RecurringInvoiceSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.deleteSchedule(scheduleId);

        assertThat(mockSchedule.getIsDeleted()).isTrue();
        assertThat(mockSchedule.getDeletedAt()).isNotNull();
        verify(scheduleRepository, times(1)).save(mockSchedule);
    }
}
