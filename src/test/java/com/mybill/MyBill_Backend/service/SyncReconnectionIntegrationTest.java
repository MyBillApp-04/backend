package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.sync.SyncChangeDto;
import com.mybill.MyBill_Backend.dto.sync.SyncRequest;
import com.mybill.MyBill_Backend.dto.sync.SyncResponse;
import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.SyncDeviceStateRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SyncReconnectionIntegrationTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SyncDeviceStateRepository deviceStateRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Sync User")
                .email("sync-reconnect@example.com")
                .password("test-only")
                .provider(AuthProvider.LOCAL)
                .role(Role.CLIENT)
                .build());

        var authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of());
        authentication.setDetails(user.getId());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deviceStateRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void reconnectRetriesInterruptedPushWithoutDuplicatingDataAndResumesPull() {
        UUID clientId = UUID.randomUUID();
        LocalDateTime clientChangedAt = LocalDateTime.now().minusMinutes(1);
        SyncChangeDto pendingChange = clientChange(clientId, clientChangedAt);

        SyncResponse firstAttempt = syncService.sync(SyncRequest.builder()
                .deviceId("android-1")
                .changes(List.of(pendingChange))
                .pageSize(100)
                .build());

        // The client lost the first response, reconnects, and retries its durable outbox.
        SyncResponse reconnect = syncService.sync(SyncRequest.builder()
                .deviceId("android-1")
                .lastPulledAt(firstAttempt.getServerTime())
                .changes(List.of(pendingChange))
                .conflictPolicy("CLIENT_WINS")
                .pageSize(100)
                .build());

        assertThat(reconnect.getRejected()).isEmpty();
        assertThat(reconnect.getAcceptedChangeIds()).containsExactly("change-1");
        assertThat(clientRepository.findByIdAndUserId(clientId, user.getId()))
                .get()
                .extracting(Client::getName)
                .isEqualTo("RECONNECTED CLIENT");
        assertThat(clientRepository.findByUserId(user.getId())).hasSize(1);

        var state = deviceStateRepository.findByUserIdAndDeviceId(user.getId(), "android-1");
        assertThat(state).isPresent();
        assertThat(state.get().getLastPushedAt()).isNotNull();
        assertThat(state.get().getLastPulledAt())
                .isCloseTo(reconnect.getServerTime(), within(1, ChronoUnit.MICROS));
        assertThat(state.get().getLastSeenAt())
                .isCloseTo(reconnect.getServerTime(), within(1, ChronoUnit.MICROS));
    }

    private SyncChangeDto clientChange(UUID clientId, LocalDateTime createdAt) {
        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("change-1");
        change.setEntityType("client");
        change.setEntityId(clientId.toString());
        change.setOperation("upsert");
        change.setCreatedAt(createdAt);
        change.setPayload(Map.of(
                "id", clientId.toString(),
                "name", "Reconnected Client",
                "email", "client@example.com",
                "isDeleted", false
        ));
        return change;
    }

    private static org.assertj.core.data.TemporalUnitOffset within(
            long value, ChronoUnit unit) {
        return org.assertj.core.api.Assertions.within(value, unit);
    }
}
