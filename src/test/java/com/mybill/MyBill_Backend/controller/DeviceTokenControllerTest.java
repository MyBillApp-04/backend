package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import com.mybill.MyBill_Backend.repository.UserDeviceTokenRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenControllerTest {

    @Mock
    private UserDeviceTokenRepository deviceTokenRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DeviceTokenController deviceTokenController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(10L).name("App Owner").email("owner@test.com").build();
    }

    @Test
    @DisplayName("registerDeviceToken saves new token for authenticated user")
    void testRegisterDeviceTokenNew() {
        when(securityUtils.getCurrentUser()).thenReturn(sampleUser);
        when(deviceTokenRepository.findByUserIdAndFcmToken(10L, "fcm-token-12345")).thenReturn(Optional.empty());

        ResponseEntity<?> response = deviceTokenController.registerDeviceToken(
                Map.of("fcmToken", "fcm-token-12345", "platform", "ANDROID"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(deviceTokenRepository).save(any(UserDeviceToken.class));
    }

    @Test
    @DisplayName("registerDeviceToken updates existing token platform")
    void testRegisterDeviceTokenExisting() {
        when(securityUtils.getCurrentUser()).thenReturn(sampleUser);
        UserDeviceToken existing = UserDeviceToken.builder().user(sampleUser).fcmToken("fcm-token-12345").platform("UNKNOWN").build();
        when(deviceTokenRepository.findByUserIdAndFcmToken(10L, "fcm-token-12345")).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = deviceTokenController.registerDeviceToken(
                Map.of("fcmToken", "fcm-token-12345", "platform", "IOS"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(existing.getPlatform()).isEqualTo("IOS");
        verify(deviceTokenRepository).save(existing);
    }

    @Test
    @DisplayName("unregisterDeviceToken deletes token for authenticated user")
    void testUnregisterDeviceToken() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);

        ResponseEntity<?> response = deviceTokenController.unregisterDeviceToken("fcm-token-12345");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(deviceTokenRepository).deleteByUserIdAndFcmToken(10L, "fcm-token-12345");
    }
}
