package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.repository.RecurringInvoiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringInvoiceScheduler {

    private final RecurringInvoiceScheduleRepository scheduleRepository;
    private final RecurringInvoiceSchedulerSelfProxy selfProxy;
    private final DatabaseLockService databaseLockService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final int SCHEDULE_FETCH_BATCH_SIZE = 25;

    @Scheduled(cron = "${app.recurring.invoices.cron:0 0 1 * * *}") // Defaults to daily at 1 AM
    public void processRecurringSchedules() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping recurring invoice run because the previous run is still active");
            return;
        }
        if (!databaseLockService.tryLock(DatabaseLockService.RECURRING_INVOICE_SCHEDULER)) {
            log.info("Skipping recurring invoice run because another instance owns the database scheduler lock");
            running.set(false);
            return;
        }

        try {
        log.info("Starting scheduled execution run for automated recurring invoices");

        LocalDateTime now = LocalDateTime.now();
        List<UUID> activeScheduleIds = scheduleRepository.findDueIds("ACTIVE", now);

        log.info("Found {} active recurring billing schedules due for processing", activeScheduleIds.size());

        for (int start = 0; start < activeScheduleIds.size(); start += SCHEDULE_FETCH_BATCH_SIZE) {
            List<UUID> batchIds = activeScheduleIds.subList(start, Math.min(start + SCHEDULE_FETCH_BATCH_SIZE, activeScheduleIds.size()));
            Map<UUID, RecurringInvoiceSchedule> schedulesById = new HashMap<>();
            scheduleRepository.findByIdIn(batchIds)
                    .forEach(schedule -> schedulesById.put(schedule.getId(), schedule));

            for (UUID scheduleId : batchIds) {
                try {
                    RecurringInvoiceSchedule schedule = schedulesById.get(scheduleId);
                    if (schedule != null) {
                        selfProxy.processSingleSchedule(schedule, now);
                    }
                } catch (Exception e) {
                    log.error("Failed to process recurring billing schedule: ID={} exception={} message={}",
                            scheduleId, e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
                }
            }
        }
        } finally {
            databaseLockService.unlock(DatabaseLockService.RECURRING_INVOICE_SCHEDULER);
            running.set(false);
        }
    }
}
