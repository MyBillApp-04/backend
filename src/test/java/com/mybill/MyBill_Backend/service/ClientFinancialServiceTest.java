package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ReceivePaymentRequest;
import com.mybill.MyBill_Backend.dto.ReceivePaymentResponse;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.ClientLedgerEntryRepository;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.ConcurrentModificationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFinancialServiceTest {

    @Mock
    private ClientLedgerEntryRepository ledgerRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FraudDetectionService fraudDetectionService;
    @Mock
    private com.mybill.MyBill_Backend.observability.AppMetrics appMetrics;
    @Mock
    private DatabaseLockService databaseLockService;

    @InjectMocks
    private ClientFinancialService clientFinancialService;

    private UUID clientId;
    private Long userId;
    private User user;
    private Client client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientId = UUID.randomUUID();
        userId = 1L;
        user = User.builder().id(userId).email("owner@example.com").build();
        client = Client.builder().id(clientId).user(user).name("Test Client").build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        // AppMetrics mocks to avoid NullPointerException
        io.micrometer.core.instrument.Counter counter = mock(io.micrometer.core.instrument.Counter.class);
        when(appMetrics.getPaymentsReceived()).thenReturn(counter);
    }

    @Test
    void receivePaymentAcquiresLockAndValidatesMath() {
        when(databaseLockService.tryLock(anyLong())).thenReturn(true);
        when(clientRepository.findByIdAndUserIdAndIsDeletedFalseWithLock(clientId, userId))
                .thenReturn(Optional.of(client));
        when(clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, userId))
                .thenReturn(Optional.of(client));

        ReceivePaymentRequest request = ReceivePaymentRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .paymentMode(PaymentMode.CASH)
                .deviceId("device-1")
                .notes("Monthly payment")
                .build();

        Payment payment = Payment.builder().paymentId(UUID.randomUUID()).amount(100.00).build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(invoiceRepository.findPendingInvoicesByClient(clientId, userId)).thenReturn(Collections.emptyList());

        ReceivePaymentResponse response = clientFinancialService.receivePayment(clientId, request);

        assertThat(response.getReceivedAmount()).isEqualTo(100.00);
        assertThat(response.getAddedToAdvance()).isEqualTo(100.00);
        assertThat(response.getAppliedToInvoices()).isEqualTo(0.0);

        verify(databaseLockService).tryLock(clientId.getMostSignificantBits());
        verify(databaseLockService).unlock(clientId.getMostSignificantBits());
    }

    @Test
    void receivePaymentThrowsExceptionWhenLockFails() {
        when(databaseLockService.tryLock(anyLong())).thenReturn(false);

        ReceivePaymentRequest request = ReceivePaymentRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .build();

        assertThatThrownBy(() -> clientFinancialService.receivePayment(clientId, request))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("Concurrent operations on client");

        verify(databaseLockService, never()).unlock(anyLong());
    }

    @Test
    void applyAdvanceToInvoiceAcquiresLockAndValidatesMath() {
        when(databaseLockService.tryLock(anyLong())).thenReturn(true);
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .invoiceNumber("INV-001")
                .totalAmount(100.00)
                .pendingAmount(100.00)
                .build();

        when(ledgerRepository.getAdvanceBalance(clientId, userId)).thenReturn(150.00);

        double applied = clientFinancialService.applyAdvanceToInvoice(invoice, 100.00, LocalDateTime.now());

        assertThat(applied).isEqualTo(100.00);
        verify(databaseLockService).tryLock(clientId.getMostSignificantBits());
        verify(databaseLockService).unlock(clientId.getMostSignificantBits());
    }
}
