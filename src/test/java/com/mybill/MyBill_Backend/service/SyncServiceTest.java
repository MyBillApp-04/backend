package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.*;
import com.mybill.MyBill_Backend.dto.sync.SyncRequest;
import com.mybill.MyBill_Backend.dto.sync.SyncResponse;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyncServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientWorkRepository workRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private ClientLedgerEntryRepository ledgerEntryRepository;
    @Mock private QuotationRepository quotationRepository;
    @Mock private QuotationItemRepository quotationItemRepository;
    @Mock private SyncDeviceStateRepository syncDeviceStateRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private ObjectMapper objectMapper;
    @Mock private InvoiceNumberService invoiceNumberService;
    @Mock private QuotationService quotationService;
    @Spy private io.micrometer.core.instrument.MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @InjectMocks
    private SyncService syncService;

    private Long userId;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = 1L;
        user = User.builder().id(userId).email("owner@example.com").build();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void doSyncExecutesSuccessfullyEmptyPayload() {
        SyncRequest request = new SyncRequest();
        request.setDeviceId("device-1");
        request.setChanges(Collections.emptyList());
        request.setConflictPolicy("CLIENT_WINS");
        request.setLastPulledAt(null);
        request.setPageSize(20);

        // Mocks for pull phase
        Page<Client> emptyClientPage = new PageImpl<>(Collections.emptyList());
        when(clientRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(emptyClientPage);
        when(workRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(invoiceRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(invoiceItemRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(ledgerEntryRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(quotationRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(quotationItemRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        SyncResponse response = syncService.sync(request);

        assertThat(response).isNotNull();
        assertThat(response.getAcceptedChangeIds()).isEmpty();
        assertThat(response.getRejected()).isEmpty();
        verify(syncDeviceStateRepository).updateSyncState(eq(userId), eq("device-1"), any(LocalDateTime.class), eq(false), eq(0));
    }
}
