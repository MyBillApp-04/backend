package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.InvoiceItem;
import com.mybill.MyBill_Backend.entity.PaymentMode;
import com.mybill.MyBill_Backend.entity.PaymentStatus;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.exception.ConflictException;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.observability.AppMetrics;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private ClientWorkRepository workRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InvoiceNumberService invoiceNumberService;
    @Mock
    private ClientFinancialService clientFinancialService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private AppMetrics appMetrics;
    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Long userId;
    private UUID clientId;
    private User user;
    private Client client;
    private ClientWork work;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = 42L;
        clientId = UUID.randomUUID();
        user = User.builder().id(userId).email("owner@example.com").build();
        client = Client.builder().id(clientId).user(user).name("Acme").state("Maharashtra").build();
        work = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .description("Fitting")
                .rate(500.0)
                .quantity(2)
                .amount(1000.0)
                .billed(false)
                .isDeleted(false)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clientRepository.findByIdAndUserIdAndIsDeletedFalseWithLock(clientId, userId))
                .thenReturn(Optional.of(client));
        when(workRepository.findAllById(any())).thenReturn(List.of(work));
        when(invoiceItemRepository.existsByWorkIdAndUserIdAndIsDeletedFalse(any(), anyLong())).thenReturn(false);
        when(invoiceNumberService.generateNextInvoiceNumber(anyLong(), any()))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult("INV-2026-0001", "2026-2027", 1, 14, "GKE", "Due in 14 days", null));
        when(clientFinancialService.getAdvanceBalance(clientId, userId)).thenReturn(0.0);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        io.micrometer.core.instrument.Counter counter = mock(io.micrometer.core.instrument.Counter.class);
        when(appMetrics.getInvoicesGenerated()).thenReturn(counter);
        when(appMetrics.getInvoicesFullyPaid()).thenReturn(counter);
    }

    private Invoice generate() {
        return invoiceService.generateInvoice(clientId, List.of(work.getId()), 0.0, null, null, 18.0, null);
    }

    @Test
    void generateRejectsWorkBelongingToAnotherClient() {
        Client otherClient = Client.builder().id(UUID.randomUUID()).user(user).build();
        ClientWork foreign = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(otherClient)
                .user(user)
                .amount(100.0)
                .billed(false)
                .isDeleted(false)
                .build();
        when(workRepository.findAllById(any())).thenReturn(List.of(foreign));

        assertThatThrownBy(() -> invoiceService.generateInvoice(clientId, List.of(foreign.getId()), 0.0, null, null, 18.0, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong to client");
    }

    @Test
    void generateRejectsWorkAlreadyBilled() {
        ClientWork billed = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .amount(100.0)
                .billed(true)
                .isDeleted(false)
                .build();
        when(workRepository.findAllById(any())).thenReturn(List.of(billed));

        assertThatThrownBy(() -> invoiceService.generateInvoice(clientId, List.of(billed.getId()), 0.0, null, null, 18.0, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been billed");
    }

    @Test
    void generateThrowsWhenWorkCountMismatches() {
        when(workRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> invoiceService.generateInvoice(clientId, List.of(work.getId()), 0.0, null, null, 18.0, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateRejectsDiscountGreaterThanSubtotal() {
        assertThatThrownBy(() -> invoiceService.generateInvoice(clientId, List.of(work.getId()), 2000.0, null, null, 18.0, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Discount cannot be greater than subtotal");
    }

    @Test
    void generateMarksWorksBilledAndRecordsLedgerEntry() {
        Invoice invoice = generate();

        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(invoice.getSubtotal()).isEqualTo(1000.0);
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(work.getBilled()).isTrue();
        assertThat(work.getInvoice()).isEqualTo(invoice);
        verify(workRepository).saveAll(List.of(work));
        verify(clientFinancialService).recordInvoiceCreated(invoice, invoice.getInvoiceDate());
        verify(auditTrailService).logChange(eq("Invoice"), eq(invoice.getId()), eq("CREATE"), any(String.class));
    }

    @Test
    void generateAppliesAdvanceWhenAvailable() {
        when(clientFinancialService.getAdvanceBalance(clientId, userId)).thenReturn(400.0);

        Invoice invoice = generate();

        assertThat(invoice.getAdvanceApplied()).isEqualTo(400.0);
        verify(clientFinancialService).applyAdvanceToInvoice(invoice, 400.0, invoice.getInvoiceDate());
    }

    @Test
    void paymentUpdateCapsAtTotalAndDerivesPendingStatus() {
        Invoice invoice = invoiceWithTotal(1000.0);

        when(invoiceRepository.findByIdAndUserIdWithLock(any(), anyLong())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice updated = invoiceService.updatePayment(invoice.getId(), 999999.0, PaymentMode.UPI, LocalDateTime.now());

        assertThat(updated.getPaidAmount()).isEqualTo(1000.0);
        assertThat(updated.getPendingAmount()).isEqualTo(0.0);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(appMetrics.getInvoicesFullyPaid()).increment();
    }

    @Test
    void paymentUpdateRejectsNegativeAmount() {
        Invoice invoice = invoiceWithTotal(1000.0);
        when(invoiceRepository.findByIdAndUserIdWithLock(any(), anyLong())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.updatePayment(invoice.getId(), -5.0, PaymentMode.CASH, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addPaymentAccumulatesOntoExistingPaidAmount() {
        Invoice invoice = invoiceWithTotal(1000.0);
        invoice.setPaidAmount(300.0);
        when(invoiceRepository.findByIdAndUserIdWithLock(any(), anyLong())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice updated = invoiceService.addPaymentForUser(invoice.getId(), userId, 200.0, PaymentMode.CASH, null);

        assertThat(updated.getPaidAmount()).isEqualTo(500.0);
        assertThat(updated.getPendingAmount()).isEqualTo(500.0);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    }

    @Test
    void subtractPaymentNeverGoesBelowZero() {
        Invoice invoice = invoiceWithTotal(1000.0);
        invoice.setPaidAmount(100.0);
        when(invoiceRepository.findByIdAndUserIdWithLock(any(), anyLong())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice updated = invoiceService.subtractPaymentForUser(invoice.getId(), userId, 500.0, PaymentMode.CASH, null);

        assertThat(updated.getPaidAmount()).isEqualTo(0.0);
        assertThat(updated.getPendingAmount()).isEqualTo(1000.0);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void deleteInvoiceReleasesAdvanceAndUnmarksWorks() {
        Invoice invoice = invoiceWithTotal(1000.0);
        invoice.setAdvanceApplied(250.0);
        ClientWork linkedWork = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .amount(1000.0)
                .billed(true)
                .invoice(invoice)
                .isDeleted(false)
                .build();
        InvoiceItem item = InvoiceItem.builder().work(linkedWork).invoice(invoice).amount(1000.0).build();
        invoice.setItems(List.of(item));

        when(invoiceRepository.findByIdAndUserId(invoice.getId(), userId)).thenReturn(Optional.of(invoice));
        when(workRepository.findAllById(any())).thenReturn(List.of(linkedWork));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceItemRepository.saveAll(any())).thenReturn(invoice.getItems());

        invoiceService.deleteInvoice(invoice.getId());

        assertThat(linkedWork.getBilled()).isFalse();
        assertThat(linkedWork.getInvoice()).isNull();
        assertThat(invoice.getIsDeleted()).isTrue();
        assertThat(item.getIsDeleted()).isTrue();
        verify(clientFinancialService).releaseAdvanceFromInvoice(clientId, userId, 250.0, invoice.getUpdatedAt(), "INV-2026-0001");
    }

    @Test
    void deleteInvoiceDoesNotReleaseWhenNoAdvanceApplied() {
        Invoice invoice = invoiceWithTotal(1000.0);
        when(invoiceRepository.findByIdAndUserId(invoice.getId(), userId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.deleteInvoice(invoice.getId());

        verify(clientFinancialService, never())
                .releaseAdvanceFromInvoice(any(), anyLong(), any(Double.class), any(), any());
    }

    private Invoice invoiceWithTotal(double total) {
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .invoiceNumber("INV-2026-0001")
                .subtotal(total)
                .grossAmount(total)
                .total(total)
                .totalAmount(total)
                .paidAmount(0.0)
                .pendingAmount(total)
                .remainingAmount(total)
                .netPayable(total)
                .paymentStatus(PaymentStatus.UNPAID)
                .invoiceDate(LocalDateTime.now())
                .build();
        return invoice;
    }
}
