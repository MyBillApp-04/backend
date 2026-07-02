package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.RecurringInvoiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
public class RecurringInvoiceScheduler {

    private final RecurringInvoiceScheduleRepository scheduleRepository;
    private final ClientWorkRepository workRepository;
    private final InvoiceService invoiceService;
    private final RecurringInvoiceSchedulerSelfProxy selfProxy;

    @Scheduled(cron = "${app.recurring.invoices.cron:0 0 1 * * *}") // Defaults to daily at 1 AM
    public void processRecurringSchedules() {
        log.info("Starting scheduled execution run for automated recurring invoices");

        LocalDateTime now = LocalDateTime.now();
        List<RecurringInvoiceSchedule> activeSchedules = scheduleRepository
                .findByStatusAndNextRunDateBeforeAndIsDeletedFalse("ACTIVE", now);

        log.info("Found {} active recurring billing schedules due for processing", activeSchedules.size());

        for (RecurringInvoiceSchedule schedule : activeSchedules) {
            try {
                selfProxy.processSingleSchedule(schedule, now);
            } catch (Exception e) {
                log.error("Failed to process recurring billing schedule for ID: {}", schedule.getId(), e);
            }
        }
    }
}
