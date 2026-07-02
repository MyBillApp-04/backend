package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.RecurringInvoiceScheduleDTO;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.RecurringInvoiceScheduleRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringInvoiceScheduleService {

    private final RecurringInvoiceScheduleRepository scheduleRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final RecurringInvoiceSchedulerSelfProxy schedulerSelfProxy;

    public List<RecurringInvoiceSchedule> getSchedulesForUser() {
        Long userId = securityUtils.getCurrentUserId();
        return scheduleRepository.findByUserIdAndIsDeletedFalse(userId);
    }

    public RecurringInvoiceSchedule getScheduleById(UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        RecurringInvoiceSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        if (!schedule.getUser().getId().equals(userId) || Boolean.TRUE.equals(schedule.getIsDeleted())) {
            throw new RuntimeException("Access denied or schedule deleted");
        }
        return schedule;
    }

    public RecurringInvoiceSchedule createSchedule(RecurringInvoiceScheduleDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Client client = clientRepository.findByIdAndUserIdAndIsDeletedFalse(dto.getClientId(), userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = schedulerSelfProxy.calculateNextRunDate(dto.getCronExpression(), dto.getBillingCycle(), now);

        RecurringInvoiceSchedule schedule = RecurringInvoiceSchedule.builder()
                .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
                .client(client)
                .user(user)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .billingCycle(dto.getBillingCycle())
                .cronExpression(dto.getCronExpression() != null ? dto.getCronExpression() : "0 0 1 1 * ?")
                .status("ACTIVE")
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : now.toLocalDate())
                .endDate(dto.getEndDate())
                .nextRunDate(nextRun)
                .autoCharge(dto.getAutoCharge() != null ? dto.getAutoCharge() : false)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .version(1)
                .build();

        return scheduleRepository.save(schedule);
    }

    public RecurringInvoiceSchedule updateSchedule(UUID id, RecurringInvoiceScheduleDTO dto) {
        RecurringInvoiceSchedule schedule = getScheduleById(id);

        LocalDateTime now = LocalDateTime.now();
        schedule.setDescription(dto.getDescription());
        schedule.setAmount(dto.getAmount());
        schedule.setBillingCycle(dto.getBillingCycle());
        schedule.setCronExpression(dto.getCronExpression());
        schedule.setEndDate(dto.getEndDate());
        schedule.setAutoCharge(dto.getAutoCharge());
        
        LocalDateTime nextRun = schedulerSelfProxy.calculateNextRunDate(dto.getCronExpression(), dto.getBillingCycle(), now);
        schedule.setNextRunDate(nextRun);
        schedule.setUpdatedAt(now);
        schedule.setVersion(schedule.getVersion() + 1);

        return scheduleRepository.save(schedule);
    }

    public RecurringInvoiceSchedule setScheduleStatus(UUID id, String status) {
        RecurringInvoiceSchedule schedule = getScheduleById(id);
        schedule.setStatus(status);
        schedule.setUpdatedAt(LocalDateTime.now());
        schedule.setVersion(schedule.getVersion() + 1);
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(UUID id) {
        RecurringInvoiceSchedule schedule = getScheduleById(id);
        schedule.setIsDeleted(true);
        schedule.setDeletedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        schedule.setVersion(schedule.getVersion() + 1);
        scheduleRepository.save(schedule);
    }
}
