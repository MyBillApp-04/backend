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
                title = "Quotation Declined";
                body = safeClient + " declined quotation " + safeQuoteNo;
                statusStr = "REJECTED";
                break;
            case "DISCUSS":
            case "DISCUSSION":
            case "DISCUSSION_REQUESTED":
                title = "Discussion Requested";
                body = safeClient + " requested discussion on quotation " + safeQuoteNo;
                if (discussionMessage != null && !discussionMessage.isBlank()) {
                    body += ": \"" + discussionMessage.trim() + "\"";
                }
                statusStr = "DISCUSSION_REQUESTED";
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

        for (UserDeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", "quotation_response")
                        .putData("quotationId", quotation.getId().toString())
                        .putData("quotationNumber", safeQuoteNo)
                        .putData("status", statusStr)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("default")
                                        .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                        .build())
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setSound("default")
                                        .build())
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                logger.info("Successfully sent FCM notification to user id {}, messageId={}", userId, response);
            } catch (FirebaseMessagingException e) {
                logger.warn("Failed to send FCM notification to token for user id {}: code={}, message={}",
                        userId, e.getMessagingErrorCode(), e.getMessage());
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    logger.info("Removing invalid/unregistered FCM token for user id {}", userId);
                    deviceTokenRepository.delete(deviceToken);
                }
            } catch (Exception e) {
                logger.error("Unexpected error sending FCM notification for user id {}: {}", userId, e.getMessage(), e);
            }
        }
    }
}
