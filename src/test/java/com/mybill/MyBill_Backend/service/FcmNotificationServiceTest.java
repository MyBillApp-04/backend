package com.mybill.MyBill_Backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.Notification;
import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import com.mybill.MyBill_Backend.repository.NotificationRepository;
import com.mybill.MyBill_Backend.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmNotificationServiceTest {

    private UserDeviceTokenRepository deviceTokenRepository;
    private NotificationRepository notificationRepository;
    private FcmNotificationService service;
    private MockedStatic<FirebaseApp> firebaseAppStatic;
    private MockedStatic<FirebaseMessaging> messagingStatic;

    private User user;
    private Client client;
    private Quotation quotation;
    private List<Notification> savedNotifications;

    @BeforeEach
    void setUp() {
        deviceTokenRepository = mock(UserDeviceTokenRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        service = new FcmNotificationService(deviceTokenRepository, notificationRepository);

        user = User.builder().id(5L).email("owner@example.com").build();
        client = Client.builder().id(UUID.randomUUID()).name("Acme").build();
        quotation = Quotation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .client(client)
                .quotationNumber("Q-1001")
                .build();

        savedNotifications = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> {
                    savedNotifications.add(inv.getArgument(0));
                    return inv.getArgument(0);
                });
    }

    @AfterEach
    void tearDown() {
        if (firebaseAppStatic != null) firebaseAppStatic.close();
        if (messagingStatic != null) messagingStatic.close();
    }

    @Test
    void nullQuotationIsIgnoredSilently() {
        service.sendQuotationResponseNotification(null, "ACCEPT", "Acme", null);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void savesInAppNotificationForAcceptAction() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of());

        service.sendQuotationResponseNotification(quotation, "ACCEPT", "Acme", null);

        assertThat(savedNotifications).hasSize(1);
        Notification notif = savedNotifications.get(0);
        assertThat(notif.getTitle()).isEqualTo("Quotation Accepted");
        assertThat(notif.getMessage()).isEqualTo("Acme accepted quotation Q-1001");
        assertThat(notif.getUser()).isSameAs(user);
        assertThat(notif.getIsRead()).isFalse();
    }

    @Test
    void declineActionMapsToRejectedMessage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of());

        service.sendQuotationResponseNotification(quotation, "DECLINE", "Acme", null);

        assertThat(savedNotifications).hasSize(1);
        assertThat(savedNotifications.get(0).getTitle()).isEqualTo("Quotation Rejected");
    }

    @Test
    void discussionActionAppendsClientMessage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of());

        service.sendQuotationResponseNotification(quotation, "DISCUSS", "Acme", "Need brass fittings");

        assertThat(savedNotifications.get(0).getTitle()).isEqualTo("New Discussion");
        assertThat(savedNotifications.get(0).getMessage()).contains("Need brass fittings");
    }

    @Test
    void revisionActionMapsToRevisionRequested() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of());

        service.sendQuotationResponseNotification(quotation, "REVISE", "Acme", "Lower price");

        assertThat(savedNotifications.get(0).getTitle()).isEqualTo("Revision Requested");
        assertThat(savedNotifications.get(0).getMessage()).contains("Lower price");
    }

    @Test
    void unknownActionFallsBackToGenericTitle() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of());

        service.sendQuotationResponseNotification(quotation, "FOLLOW_UP", "Acme", null);

        assertThat(savedNotifications.get(0).getTitle()).isEqualTo("Quotation Update");
    }

    @Test
    void skipsFcmWhenFirebaseIsNotInitialized() {
        firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class);
        firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(java.util.Collections.emptyList());

        when(deviceTokenRepository.findByUserId(5L)).thenReturn(
                List.of(UserDeviceToken.builder().fcmToken("tok").build()));

        service.sendQuotationResponseNotification(quotation, "ACCEPT", "Acme", null);

        assertThat(savedNotifications).hasSize(1);
        verify(deviceTokenRepository, never()).delete(any());
    }

    @Test
    void removesUnregisteredTokenAndSendsToValidTokens() throws Exception {
        firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class);
        firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
        messagingStatic = Mockito.mockStatic(FirebaseMessaging.class);
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

        UserDeviceToken invalid = UserDeviceToken.builder().fcmToken("invalid-tok").build();
        UserDeviceToken valid = UserDeviceToken.builder().fcmToken("valid-tok").build();
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(List.of(invalid, valid));

        FirebaseMessagingException unreg = mock(FirebaseMessagingException.class);
        when(unreg.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(com.google.firebase.messaging.Message.class)))
                .thenThrow(unreg)
                .thenReturn("message-id-123");

        service.sendQuotationResponseNotification(quotation, "ACCEPT", "Acme", null);

        verify(deviceTokenRepository).delete(invalid);
        verify(messaging, Mockito.times(2)).send(any(com.google.firebase.messaging.Message.class));
        verify(deviceTokenRepository, never()).delete(valid);
        assertThat(savedNotifications).hasSize(1);
    }
}