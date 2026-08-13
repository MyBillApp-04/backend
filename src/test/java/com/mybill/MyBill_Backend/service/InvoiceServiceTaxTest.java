package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.PaymentStatus;
import com.mybill.MyBill_Backend.entity.TaxType;
import com.mybill.MyBill_Backend.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceServiceTaxTest {

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
    private com.mybill.MyBill_Backend.observability.AppMetrics appMetrics;
    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID clientId;
    private Long userId;
    private User user;
    private Client client;
    private ClientWork work;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = 99L;
        clientId = UUID.randomUUID();
        user = User.builder().id(userId).email("owner@example.com").build();
        client = Client.builder().id(clientId).user(user).name("Test Client").build();

        work = ClientWork.builder()
                .id(UUID.randomUUID())
                .client(client)
                .user(user)
                .description("Labour")
                .rate(1000.0)
                .quantity(1)
                .amount(1000.0)
                .billed(false)
                .isDeleted(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clientRepository.findByIdAndUserIdAndIsDeletedFalseWithLock(clientId, userId))
                .thenReturn(Optional.of(client));
        when(workRepository.findAllById(any())).thenReturn(List.of(work));
        when(invoiceItemRepository.existsByWorkIdAndUserIdAndIsDeletedFalse(any(), anyLong())).thenReturn(false);
        when(invoiceNumberService.generateNextInvoiceNumber(anyLong(), any()))
                .thenReturn(new InvoiceNumberService.InvoiceNumberResult("INV-TAX-001", "2026-2027", 1, 14, "", "Payment due", null));
        when(clientFinancialService.getAdvanceBalance(clientId, userId)).thenReturn(0.0);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        io.micrometer.core.instrument.Counter counter = mock(io.micrometer.core.instrument.Counter.class);
        when(appMetrics.getInvoicesGenerated()).thenReturn(counter);
    }

    @Test
    void intraState18GSTSnapshotWiredOntoInvoice() {
        client.setState("Maharashtra");
        when(businessProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(com.mybill.MyBill_Backend.entity.BusinessProfile.builder()
                        .user(user)
                        .state("Maharashtra")
                        .build()));

        Invoice invoice = invoiceService.generateInvoiceForUser(
                clientId, List.of(work.getId()), 200.0, null, null, 18.0, null, userId);

        assertThat(invoice.getSubtotal()).isEqualTo(1000.0);
        assertThat(invoice.getDiscount()).isEqualTo(200.0);
        assertThat(invoice.getGrossAmount()).isEqualTo(800.0);
        assertThat(invoice.getTaxRate()).isEqualTo(18.0);
        assertThat(invoice.getTaxType()).isEqualTo(TaxType.INTRA_STATE);
        assertThat(invoice.getTaxableAmount()).isEqualTo(800.0);
        assertThat(invoice.getTaxAmount()).isEqualTo(144.0);
        assertThat(invoice.getCgstAmount()).isEqualTo(72.0);
        assertThat(invoice.getSgstAmount()).isEqualTo(72.0);
        assertThat(invoice.getIgstAmount()).isEqualTo(0.0);
        assertThat(invoice.getTotal()).isEqualTo(944.0);
        assertThat(invoice.getNetPayable()).isEqualTo(944.0);
        assertThat(invoice.getTotalAmount()).isEqualTo(944.0);
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void interStateIgstAndAdvanceCapAppliedToTotal() {
        client.setState("Gujarat");
        when(businessProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(com.mybill.MyBill_Backend.entity.BusinessProfile.builder()
                        .user(user)
                        .state("Maharashtra")
                        .build()));
        when(clientFinancialService.getAdvanceBalance(clientId, userId)).thenReturn(500.0);

        Invoice invoice = invoiceService.generateInvoiceForUser(
                clientId, List.of(work.getId()), 0.0, null, null, 18.0, null, userId);

        assertThat(invoice.getTaxType()).isEqualTo(TaxType.INTER_STATE);
        assertThat(invoice.getTaxAmount()).isEqualTo(180.0);
        assertThat(invoice.getIgstAmount()).isEqualTo(180.0);
        assertThat(invoice.getTotal()).isEqualTo(1180.0);
        assertThat(invoice.getAdvanceApplied()).isEqualTo(500.0);
        assertThat(invoice.getNetPayable()).isEqualTo(680.0);
        assertThat(invoice.getTotalAmount()).isEqualTo(680.0);
    }

    @Test
    void noTaxPreservesLegacyTotals() {
        Invoice invoice = invoiceService.generateInvoiceForUser(
                clientId, List.of(work.getId()), 50.0, null, null, null, null, userId);

        assertThat(invoice.getTaxRate()).isEqualTo(0.0);
        assertThat(invoice.getTaxType()).isEqualTo(TaxType.NONE);
        assertThat(invoice.getGrossAmount()).isEqualTo(950.0);
        assertThat(invoice.getTotal()).isEqualTo(950.0);
        assertThat(invoice.getNetPayable()).isEqualTo(950.0);
        assertThat(invoice.getTotalAmount()).isEqualTo(950.0);
    }

    @Test
    void explicitGstTypeOverrideWins() {
        // business state set but client state missing -> without override this is INTER.
        // Explicit INTRA override forces CGST+SGST regardless.
        when(businessProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(com.mybill.MyBill_Backend.entity.BusinessProfile.builder()
                        .user(user)
                        .state("Maharashtra")
                        .build()));

        Invoice invoice = invoiceService.generateInvoiceForUser(
                clientId, List.of(work.getId()), 0.0, null, null, 18.0, TaxType.INTRA_STATE, userId);

        assertThat(invoice.getTaxType()).isEqualTo(TaxType.INTRA_STATE);
        assertThat(invoice.getCgstAmount()).isEqualTo(90.0);
        assertThat(invoice.getSgstAmount()).isEqualTo(90.0);
        assertThat(invoice.getIgstAmount()).isEqualTo(0.0);
    }
}