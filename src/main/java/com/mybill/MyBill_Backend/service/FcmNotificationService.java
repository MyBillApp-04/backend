package com.mybill.MyBill_Backend.service;

import com.google.firebase.messaging.*;
import com.mybill.MyBill_Backend.entity.Notification;
import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import com.mybill.MyBill_Backend.repository.NotificationRepository;
import com.mybill.MyBill_Backend.repository.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;

    public FcmNotificationService(UserDeviceTokenRepository deviceTokenRepository, NotificationRepository notificationRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
    }

    @Async
    @Transactional
    public void sendQuotationResponseNotification(Quotation quotation, String action, String clientName, String discussionMessage) {
        if (quotation == null || quotation.getUser() == null) {
            return;
        }

        Long userId = quotation.getUser().getId();
        String safeClient = clientName != null && !clientName.isBlank() ? clientName : "Client";
        String safeQuoteNo = quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "";

        String title;
        String body;
        String statusStr;

        switch (action.toUpperCase()) {
            case "ACCEPT":
            case "ACCEPTED":
                title = "Quotation Accepted";
                body = safeClient + " accepted quotation " + safeQuoteNo;
                statusStr = "ACCEPTED";
                break;
            case "DECLINE":
            case "DECLINED":
            case "REJECTED":
                title = "Quotation Rejected";
                body = safeClient + " rejected quotation " + safeQuoteNo;
                statusStr = "REJECTED";
                break;
            case "DISCUSS":
            case "DISCUSSION":
            case "DISCUSSION_REQUESTED":
                title = "New Discussion";
                body = safeClient + " commented on quotation " + safeQuoteNo;
                if (discussionMessage != null && !discussionMessage.isBlank()) {
                    body += ": \"" + discussionMessage.trim() + "\"";
                }
                statusStr = "DISCUSSION_REQUESTED";
                break;
            case "REVISE":
            case "REVISION":
            case "REVISION_REQUESTED":
            case "MODIFICATION":
                title = "Revision Requested";
                body = safeClient + " requested changes for quotation " + safeQuoteNo;
                if (discussionMessage != null && !discussionMessage.isBlank()) {
                    body += ": \"" + discussionMessage.trim() + "\"";
                }
                statusStr = "REVISION_REQUESTED";
                break;
            default:
                title = "Quotation Update";
                body = safeClient + " responded to quotation " + safeQuoteNo;
                statusStr = action;
                break;
        }

        // 1. Save In-App Notification entity for the business user
        try {
            Notification inAppNotif = Notification.builder()
                    .notificationId(UUID.randomUUID())
                    .user(quotation.getUser())
                    .title(title)
                    .message(body)
                    .isRead(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(inAppNotif);
            logger.info("Saved in-app notification for user id {}: {}", userId, title);
        } catch (Exception e) {
            logger.error("Failed to save in-app notification for user id {}: {}", userId, e.getMessage(), e);
        }

        // 2. Dispatch FCM Push Notification if Firebase is configured & user has device tokens
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            logger.info("FirebaseApp is not initialized. Skipping FCM push notification.");
            return;
        }

        List<UserDeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            logger.info("No FCM tokens registered for user id {}", userId);
            return;
        }

        logger.info("Notification requested: quotationId={}, action={}", quotation.getId(), action);

        for (UserDeviceToken deviceToken : tokens) {
            String tokenStr = deviceToken.getFcmToken();
            int maxAttempts = 4;
            int attempt = 0;
            boolean sent = false;
            long backoffMs = 1000;

            while (attempt < maxAttempts && !sent) {
                attempt++;
                try {
                    Message message = Message.builder()
                            .setToken(tokenStr)
                            .setNotification(com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .putData("type", "quotation_response")
                            .putData("quotationId", quotation.getId().toString())
                            .putData("quotationNumber", safeQuoteNo)
                            .putData("status", statusStr)
                            .putData("action", statusStr)
                            .putData("clientId", quotation.getClient() != null ? quotation.getClient().getId().toString() : "")
                            .putData("timestamp", java.time.Instant.now().toString())
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(AndroidNotification.builder()
                                            .setSound("default")
                                            .setChannelId("high_importance_channel")
                                            .setPriority(AndroidNotification.Priority.HIGH)
                                            .setDefaultSound(true)
                                            .setDefaultVibrateTimings(true)
                                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                            .build())
                                    .build())
                            .setApnsConfig(ApnsConfig.builder()
                                    .putHeader("apns-priority", "10")
                                    .setAps(Aps.builder()
                                            .setSound("default")
                                            .setBadge(1)
                                            .setContentAvailable(true)
                                            .build())
                                    .build())
                            .build();

                    logger.info("Payload built for user id {}: title=\"{}\", token={}", userId, title, tokenStr);

                    String response = FirebaseMessaging.getInstance().send(message);
                    logger.info("Firebase response: {}", response);
                    logger.info("Success: Successfully sent FCM notification to user id {}, messageId={}", userId, response);
                    sent = true;
                } catch (FirebaseMessagingException e) {
                    MessagingErrorCode errorCode = e.getMessagingErrorCode();
                    logger.warn("Failure: Failed to send FCM notification on attempt {}/{} for user id {}: code={}, message={}",
                            attempt, maxAttempts, userId, errorCode, e.getMessage());

                    if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        logger.info("Token invalid: Removing invalid/unregistered FCM token for user id {}: {}", userId, tokenStr);
                        deviceTokenRepository.delete(deviceToken);
                        break;
                    }

                    if (attempt < maxAttempts) {
                        logger.info("Retry: Retrying FCM notification send to user id {} in {}ms", userId, backoffMs);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        backoffMs *= 2;
                    } else {
                        logger.error("Failure: Failed to send FCM notification to user id {} after {} attempts.", userId, maxAttempts);
                    }
                } catch (Exception e) {
                    logger.error("Failure: Unexpected error sending FCM notification on attempt {}/{} for user id {}: {}",
                            attempt, maxAttempts, userId, e.getMessage(), e);
                    if (attempt < maxAttempts) {
                        logger.info("Retry: Retrying FCM notification send to user id {} in {}ms due to unexpected error", userId, backoffMs);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        backoffMs *= 2;
                    } else {
                        logger.error("Failure: Unexpected error sending FCM notification to user id {} after {} attempts.", userId, maxAttempts);
                    }
                }
            }
        }
    }
}
