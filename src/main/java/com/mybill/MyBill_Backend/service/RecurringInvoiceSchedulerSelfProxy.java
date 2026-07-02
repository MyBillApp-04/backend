package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.RecurringInvoiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringInvoiceSchedulerSelfProxy {

    private final RecurringInvoiceScheduleRepository scheduleRepository;
    private final ClientWorkRepository workRepository;
    private final InvoiceService invoiceService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleSchedule(RecurringInvoiceSchedule schedule, LocalDateTime now) {
        log.info("Processing recurring billing schedule template ID: {} for Client ID: {}",
                schedule.getId(), schedule.getClient().getId());

        // 1. Generate the ClientWork entry (unbilled initially)
        ClientWork work = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(schedule.getClient())
                .user(schedule.getUser())
                .description(schedule.getDescription())
                .rate(schedule.getAmount().doubleValue())
                .quantity(1)
                .amount(schedule.getAmount().doubleValue())
                .billed(false)
                .date(now)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .version(1)
                .build();

        ClientWork savedWork = workRepository.save(work);

        // 2. Delegate to InvoiceService to generate the invoice
        Invoice invoice = invoiceService.generateInvoiceForUser(
                schedule.getClient().getId(),
                List.of(savedWork.getId()),
                0.0,
                schedule.getDescription(),
                now.plusDays(30), // standard due date in 30 days
                schedule.getUser().getId()
        );

        log.info("Successfully auto-generated recurring invoice ID: {} for Client ID: {}",
                invoice.getId(), schedule.getClient().getId());

        // 3. Update the schedule timestamps and next run date
        LocalDateTime nextRun = calculateNextRunDate(schedule.getCronExpression(), schedule.getBillingCycle(), now);
        schedule.setLastRunDate(now);
        schedule.setNextRunDate(nextRun);
        schedule.setUpdatedAt(now);
        scheduleRepository.save(schedule);

        log.info("Updated recurring schedule ID: {} next run date set to: {}", schedule.getId(), nextRun);
    }

    public LocalDateTime calculateNextRunDate(String cronExpr, String cycle, LocalDateTime baseTime) {
        try {
            if (cronExpr != null && !cronExpr.isBlank() && CronExpression.isValidExpression(cronExpr)) {
                LocalDateTime next = CronExpression.parse(cronExpr).next(baseTime);
                if (next != null) return next;
            }
        } catch (Exception e) {
            log.warn("Invalid cron expression: {}, falling back to standard interval calculation", cronExpr);
        }

        if (cycle == null) return baseTime.plusMonths(1);
        return switch (cycle.toUpperCase()) {
            case "WEEKLY" -> baseTime.plusWeeks(1);
            case "QUARTERLY" -> baseTime.plusMonths(3);
            case "YEARLY" -> baseTime.plusYears(1);
            default -> baseTime.plusMonths(1);
        };
    }
}
