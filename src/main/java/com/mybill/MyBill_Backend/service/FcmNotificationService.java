package com.mybill.MyBill_Backend.service;

import com.google.firebase.messaging.*;
import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import com.mybill.MyBill_Backend.repository.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);

    private final UserDeviceTokenRepository deviceTokenRepository;

    public FcmNotificationService(UserDeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Async
    @Transactional
    public void sendQuotationResponseNotification(Quotation quotation, String action, String clientName, String discussionMessage) {
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            logger.info("FirebaseApp is not initialized. Skipping FCM push notification.");
            return;
        }

        if (quotation == null || quotation.getUser() == null) {
            return;
        }

        Long userId = quotation.getUser().getId();
        List<UserDeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            logger.info("No FCM tokens registered for user id {}", userId);
            return;
        }

        String title;
        String body;
        String statusStr;

        String safeClient = clientName != null && !clientName.isBlank() ? clientName : "Client";
        String safeQuoteNo = quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "";

        switch (action.toUpperCase()) {
            case "ACCEPT":
            case "ACCEPTED":
                title = "Quotation accepted";
                body = safeClient + " accepted quotation " + safeQuoteNo;
                statusStr = "ACCEPTED";
                break;
            case "DECLINE":
            case "DECLINED":
            case "REJECTED":
                title = "Quotation declined";
                body = safeClient + " declined quotation " + safeQuoteNo;
                statusStr = "REJECTED";
                break;
            case "DISCUSS":
            case "DISCUSSION":
            case "DISCUSSION_REQUESTED":
                title = "Discussion requested";
                body = safeClient + " requested discussion on quotation " + safeQuoteNo;
                if (discussionMessage != null && !discussionMessage.isBlank()) {
                    body += ": \"" + discussionMessage.trim() + "\"";
                }
                statusStr = "DISCUSSION_REQUESTED";
                break;
            default:
                title = "Quotation update";
                body = safeClient + " responded to quotation " + safeQuoteNo;
                statusStr = action;
                break;
        }

        for (UserDeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getFcmToken())
                        .setNotification(Notification.builder()
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
