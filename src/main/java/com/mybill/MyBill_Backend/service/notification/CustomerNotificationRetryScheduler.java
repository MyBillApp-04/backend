package com.mybill.MyBill_Backend.service.notification;

import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.service.DatabaseLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.retry.enabled", havingValue = "true", matchIfMissing = false)
public class CustomerNotificationRetryScheduler {

    private final CustomerNotificationService notificationService;
    private final DatabaseLockService databaseLockService;
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Value("${app.notifications.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.notifications.retry.delay-ms:300000}")
    public void retryFailedNotifications() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping failed notification retry because the previous retry is still active");
            return;
        }
        if (!databaseLockService.tryLock(DatabaseLockService.CUSTOMER_NOTIFICATION_RETRY)) {
            log.info("Skipping failed notification retry because another instance owns the database scheduler lock");
            running.set(false);
            return;
        }
        try {
            log.info("Executing scheduled retry for failed customer notifications (Max attempts: {})", maxRetries);
            notificationService.retryFailedNotifications(maxRetries);
        } catch (Exception e) {
            log.error("Error occurred while retrying failed notifications: exception={} message={}",
                    e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
        } finally {
            databaseLockService.unlock(DatabaseLockService.CUSTOMER_NOTIFICATION_RETRY);
            running.set(false);
        }
    }
}
