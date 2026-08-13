package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.*;
import com.mybill.MyBill_Backend.dto.sync.SyncChangeDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Spy private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    @Mock private InvoiceNumberService invoiceNumberService;
    @Mock private QuotationService quotationService;
    @Mock private BusinessProfileRepository businessProfileRepository;
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

    @Test
    void buildInvoiceRecomputesClientSuppliedTotalsServerSide() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).state("Maharashtra").build();
        when(clientRepository.findByIdAndUserId(eq(clientId), eq(userId))).thenReturn(Optional.of(client));
        when(invoiceRepository.findByIdAndUserId(eq(invoiceId), eq(userId))).thenReturn(Optional.empty());
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(invoiceNumberService.generateNextInvoiceNumber(eq(userId), any(LocalDate.class)))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult(
                        "INV-2026-0001", "2026-27", 1, 7, null, "note", null));

        // Client sends plausible line items but deliberately bogus financial totals.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", invoiceId.toString());
        payload.put("clientId", clientId.toString());
        payload.put("subtotal", 1000.0);
        payload.put("discount", 100.0);
        payload.put("grossAmount", 1.0);        // wrong: should be 900
        payload.put("taxRate", 18.0);
        payload.put("taxType", "INTRA_STATE");
        payload.put("totalAmount", 999999.0);   // wrong: should be 1062
        payload.put("netPayable", 0.0);         // wrong: should be 1062 after cap
        payload.put("advanceApplied", 999999.0);// wrong: should be capped at total
        payload.put("paidAmount", 999999.0);    // wrong: should be capped at net payable
        payload.put("pendingAmount", 0.0);
        payload.put("paymentStatus", "PAID");
        payload.put("invoiceDate", LocalDateTime.now());
        payload.put("deviceId", "device-1");
        payload.put("quotationId", null);

        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("chg-1");
        change.setEntityType("invoice");
        change.setEntityId(invoiceId.toString());
        change.setOperation("CREATE");
        change.setPayload(payload);
        change.setCreatedAt(LocalDateTime.now());

        Invoice result = (Invoice) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                syncService, "buildInvoice", change, user, userId, "device-1", LocalDateTime.now(), "CLIENT_WINS",
                new HashMap<>(), new HashMap<>());

        assertThat(result).isNotNull();
        assertThat(result.getGrossAmount()).isEqualTo(900.0);
        assertThat(result.getTaxableAmount()).isEqualTo(900.0);
        assertThat(result.getTaxAmount()).isEqualTo(162.0);
        assertThat(result.getTotal()).isEqualTo(1062.0);
        assertThat(result.getAdvanceApplied()).isEqualTo(1062.0); // capped at total
        assertThat(result.getNetPayable()).isEqualTo(0.0);        // total - capped advance
        assertThat(result.getTotalAmount()).isEqualTo(0.0);
        assertThat(result.getPaidAmount()).isEqualTo(0.0);        // capped at net payable
        assertThat(result.getPendingAmount()).isEqualTo(0.0);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(invoiceNumberService).generateNextInvoiceNumber(eq(userId), any(LocalDate.class));
    }

    @Test
    void buildInvoiceItemWithoutWorkIdDoesNotFail() {
        UUID itemId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).build();
        when(invoiceRepository.findByIdAndUserId(eq(invoiceId), eq(userId))).thenReturn(Optional.of(invoice));
        when(invoiceItemRepository.findByIdAndUserId(eq(itemId), eq(userId))).thenReturn(Optional.empty());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", itemId.toString());
        payload.put("invoiceId", invoiceId.toString());
        payload.put("description", "Manual line item");
        payload.put("rate", 100.0);
        payload.put("quantity", 2);
        payload.put("amount", 200.0);
        payload.put("isDeleted", false);
        payload.put("deviceId", "device-1");

        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("chg-i1");
        change.setEntityType("invoice_item");
        change.setEntityId(itemId.toString());
        change.setOperation("CREATE");
        change.setPayload(payload);
        change.setCreatedAt(LocalDateTime.now());

        InvoiceItem result = (InvoiceItem) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                syncService, "buildInvoiceItem", change, user, userId, "device-1", LocalDateTime.now());

        assertThat(result).isNotNull();
        assertThat(result.getWork()).isNull();
        assertThat(result.getInvoice().getId()).isEqualTo(invoiceId);
    }

    @Test
    void buildInvoiceItemWithUnknownWorkIdDoesNotFail() {
        UUID itemId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID workId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).build();
        when(invoiceRepository.findByIdAndUserId(eq(invoiceId), eq(userId))).thenReturn(Optional.of(invoice));
        when(workRepository.findByIdAndUserId(eq(workId), eq(userId))).thenReturn(Optional.empty());
        when(invoiceItemRepository.findByIdAndUserId(eq(itemId), eq(userId))).thenReturn(Optional.empty());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", itemId.toString());
        payload.put("invoiceId", invoiceId.toString());
        payload.put("workId", workId.toString());
        payload.put("description", "Line item with stale work reference");
        payload.put("rate", 50.0);
        payload.put("quantity", 1);
        payload.put("amount", 50.0);
        payload.put("isDeleted", false);
        payload.put("deviceId", "device-1");

        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("chg-i2");
        change.setEntityType("invoice_item");
        change.setEntityId(itemId.toString());
        change.setOperation("CREATE");
        change.setPayload(payload);
        change.setCreatedAt(LocalDateTime.now());

        InvoiceItem result = (InvoiceItem) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                syncService, "buildInvoiceItem", change, user, userId, "device-1", LocalDateTime.now());

        assertThat(result).isNotNull();
        assertThat(result.getWork()).isNull();
    }

    @Test
    void buildInvoiceItemWithLinkedWorkMarksItBilled() {
        UUID itemId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID workId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).build();
        ClientWork work = ClientWork.builder().id(workId).build();
        when(invoiceRepository.findByIdAndUserId(eq(invoiceId), eq(userId))).thenReturn(Optional.of(invoice));
        when(workRepository.findByIdAndUserId(eq(workId), eq(userId))).thenReturn(Optional.of(work));
        when(invoiceItemRepository.findByIdAndUserId(eq(itemId), eq(userId))).thenReturn(Optional.empty());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", itemId.toString());
        payload.put("invoiceId", invoiceId.toString());
        payload.put("workId", workId.toString());
        payload.put("description", "Work-linked line item");
        payload.put("rate", 50.0);
        payload.put("quantity", 1);
        payload.put("amount", 50.0);
        payload.put("isDeleted", false);
        payload.put("deviceId", "device-1");

        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("chg-i3");
        change.setEntityType("invoice_item");
        change.setEntityId(itemId.toString());
        change.setOperation("CREATE");
        change.setPayload(payload);
        change.setCreatedAt(LocalDateTime.now());

        InvoiceItem result = (InvoiceItem) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                syncService, "buildInvoiceItem", change, user, userId, "device-1", LocalDateTime.now());

        assertThat(result).isNotNull();
        assertThat(result.getWork()).isSameAs(work);
        assertThat(work.getBilled()).isTrue();
        assertThat(work.getInvoice()).isSameAs(invoice);
    }

    @Test
    void buildInvoiceRejectsDiscountGreaterThanSubtotal() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).state("Maharashtra").build();
        when(clientRepository.findByIdAndUserId(eq(clientId), eq(userId))).thenReturn(Optional.of(client));
        when(invoiceRepository.findByIdAndUserId(eq(invoiceId), eq(userId))).thenReturn(Optional.empty());
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(invoiceNumberService.generateNextInvoiceNumber(eq(userId), any(LocalDate.class)))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult(
                        "INV-2026-0001", "2026-27", 1, 7, null, "note", null));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", invoiceId.toString());
        payload.put("clientId", clientId.toString());
        payload.put("subtotal", 100.0);
        payload.put("discount", 500.0); // invalid: discount > subtotal
        payload.put("taxRate", 18.0);
        payload.put("taxType", "INTRA_STATE");
        payload.put("invoiceDate", LocalDateTime.now());
        payload.put("deviceId", "device-1");

        SyncChangeDto change = new SyncChangeDto();
        change.setChangeId("chg-2");
        change.setEntityType("invoice");
        change.setEntityId(invoiceId.toString());
        change.setOperation("CREATE");
        change.setPayload(payload);
        change.setCreatedAt(LocalDateTime.now());

        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                syncService, "buildInvoice", change, user, userId, "device-1", LocalDateTime.now(), "CLIENT_WINS",
                new HashMap<>(), new HashMap<>()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Discount cannot be greater than subtotal");
    }
}
