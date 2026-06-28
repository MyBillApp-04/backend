package com.mybill.MyBill_Backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerNotificationRetryScheduler {

    private final CustomerNotificationService notificationService;

    @Value("${app.notifications.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.notifications.retry.delay-ms:300000}")
    public void retryFailedNotifications() {
        try {
            log.info("Executing scheduled retry for failed customer notifications (Max attempts: {})", maxRetries);
            notificationService.retryFailedNotifications(maxRetries);
        } catch (Exception e) {
            log.error("Error occurred while retrying failed notifications: {}", e.getMessage(), e);
        }
    }
}
