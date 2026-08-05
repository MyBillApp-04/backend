package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.repository.QuotationResponseEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationPublicResponseServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationResponseEventRepository responseEventRepository;

    @Mock
    private FcmNotificationService fcmNotificationService;

    @InjectMocks
    private QuotationPublicResponseService publicResponseService;

    private Quotation sampleQuotation;
    private User sampleUser;
    private Client sampleClient;
    private String rawToken;
    private String tokenHash;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).name("Test Business Owner").email("owner@test.com").build();
        sampleClient = Client.builder().id(UUID.randomUUID()).name("Valued Client").build();
        rawToken = QuotationPublicResponseService.generateRandomToken();
        tokenHash = QuotationPublicResponseService.hashToken(rawToken);

        sampleQuotation = Quotation.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .client(sampleClient)
                .quotationNumber("QT-2425-0001")
                .status(QuotationStatus.SENT)
                .clientResponseStatus("PENDING")
                .publicTokenHash(tokenHash)
                .tokenCreatedAt(LocalDateTime.now())
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .totalAmount(1500.00)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Hash token generates consistent SHA-256 hex string")
    void testHashToken() {
        String token = QuotationPublicResponseService.generateRandomToken();
        String hash1 = QuotationPublicResponseService.hashToken(token);
        String hash2 = QuotationPublicResponseService.hashToken(token);

        assertThat(hash1).isNotNull();
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1.length()).isEqualTo(64); // 256 bits = 64 hex chars
    }

    @Test
    @DisplayName("getPublicQuotationView DOES NOT mutate quotation status or audit log (GET side-effect free)")
    void testGetPublicQuotationViewIsSideEffectFree() {
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.PublicQuotationView view =
                publicResponseService.getPublicQuotationView(rawToken);

        assertThat(view.isValid()).isTrue();
        assertThat(view.quotationNumber()).isEqualTo("QT-2425-0001");
        assertThat(view.clientName()).isEqualTo("Valued Client");
        assertThat(view.mainStatus()).isEqualTo("SENT");
        assertThat(view.clientResponseStatus()).isEqualTo("PENDING");

        verify(quotationRepository, never()).save(any());
        verify(responseEventRepository, never()).save(any());
        verify(fcmNotificationService, never()).sendQuotationResponseNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processClientResponse ACCEPT updates status to ACCEPTED, saves audit log, and notifies owner")
    void testProcessClientResponseAccept() {
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(rawToken, "ACCEPT", null, "127.0.0.1", "TestAgent");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(sampleQuotation.getStatus()).isEqualTo(QuotationStatus.ACCEPTED);
        assertThat(sampleQuotation.getClientResponseStatus()).isEqualTo("ACCEPTED");

        verify(quotationRepository).save(sampleQuotation);
        verify(responseEventRepository).save(any(QuotationResponseEvent.class));
        verify(fcmNotificationService).sendQuotationResponseNotification(eq(sampleQuotation), eq("ACCEPTED"), eq("Valued Client"), any());
    }

    @Test
    @DisplayName("processClientResponse DECLINE updates status to REJECTED")
    void testProcessClientResponseDecline() {
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(rawToken, "DECLINE", null, "127.0.0.1", "TestAgent");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("DECLINED");
        assertThat(sampleQuotation.getStatus()).isEqualTo(QuotationStatus.REJECTED);
        assertThat(sampleQuotation.getClientResponseStatus()).isEqualTo("DECLINED");
    }

    @Test
    @DisplayName("processClientResponse DISCUSS updates discussion state without overwriting terminal main status")
    void testProcessClientResponseDiscuss() {
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(rawToken, "DISCUSS", "Can we get 5% discount?", "127.0.0.1", "TestAgent");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("DISCUSSION_REQUESTED");
        assertThat(sampleQuotation.getStatus()).isEqualTo(QuotationStatus.DISCUSSION_REQUESTED);
        assertThat(sampleQuotation.getDiscussionMessage()).isEqualTo("Can we get 5% discount?");
    }

    @Test
    @DisplayName("Expired token is rejected")
    void testExpiredTokenHandling() {
        sampleQuotation.setTokenExpiresAt(LocalDateTime.now().minusDays(1));
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.PublicQuotationView view = publicResponseService.getPublicQuotationView(rawToken);
        assertThat(view.isExpired()).isTrue();

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(rawToken, "ACCEPT", null, "127.0.0.1", "TestAgent");
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("processClientResponse REVISE updates status to DISCUSSION_REQUESTED and clientResponseStatus to REVISION_REQUESTED")
    void testProcessClientResponseRevise() {
        when(quotationRepository.findByPublicTokenHash(tokenHash)).thenReturn(Optional.of(sampleQuotation));

        QuotationPublicResponseService.ResponseSubmissionResult result =
                publicResponseService.processClientResponse(rawToken, "REVISE", "Please change quantity to 10", "127.0.0.1", "TestAgent");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("REVISION_REQUESTED");
        assertThat(sampleQuotation.getStatus()).isEqualTo(QuotationStatus.DISCUSSION_REQUESTED);
        assertThat(sampleQuotation.getClientResponseStatus()).isEqualTo("REVISION_REQUESTED");
        assertThat(sampleQuotation.getDiscussionMessage()).isEqualTo("Please change quantity to 10");

        verify(quotationRepository).save(sampleQuotation);
        verify(responseEventRepository).save(any(QuotationResponseEvent.class));
        verify(fcmNotificationService).sendQuotationResponseNotification(eq(sampleQuotation), eq("REVISION_REQUESTED"), eq("Valued Client"), eq("Please change quantity to 10"));
    }

    @Test
    @DisplayName("A quotation UUID cannot be used as a public capability token")
    void rejectsRawQuotationUuidWithoutLookingItUp() {
        QuotationPublicResponseService.PublicQuotationView view =
                publicResponseService.getPublicQuotationView(sampleQuotation.getId().toString());

        assertThat(view.isValid()).isFalse();
        verify(quotationRepository, never()).findById(sampleQuotation.getId());
        verify(quotationRepository, never()).save(any());
    }
}
