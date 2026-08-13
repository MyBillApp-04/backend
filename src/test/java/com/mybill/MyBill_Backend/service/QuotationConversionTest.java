package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.QuotationItemRepository;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuotationConversionTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationItemRepository quotationItemRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private InvoiceNumberService invoiceNumberService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private QuotationService quotationService;

    private User mockUser;
    private Client mockClient;
    private Quotation mockQuotation;
    private QuotationItem mockItem1;
    private QuotationItem mockItem2;
    private UUID quotationId;
    private UUID clientId;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        quotationId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        mockUser = User.builder().id(userId).email("user@example.com").build();
        mockClient = Client.builder().id(clientId).name("Acme Corp").user(mockUser).build();

        mockQuotation = Quotation.builder()
                .id(quotationId)
                .user(mockUser)
                .client(mockClient)
                .quotationNumber("QT-2627-0001")
                .status(QuotationStatus.ACCEPTED)
                .subtotal(350.0)
                .discount(50.0)
                .grossAmount(350.0)
                .totalAmount(300.0)
                .netPayable(300.0)
                .isDeleted(false)
                .version(1)
                .build();

        mockItem1 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .quotation(mockQuotation)
                .user(mockUser)
                .description("Custom Steel Plate")
                .dimension("10x20 ft")
                .quantity(2)
                .kgs(50.5)
                .amount(200.0)
                .isDeleted(false)
                .build();

        mockItem2 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .quotation(mockQuotation)
                .user(mockUser)
                .description("Standard Labor")
                .dimension(null)
                .quantity(1)
                .kgs(null)
                .amount(100.0)
                .isDeleted(false)
                .build();
    }

    @Test
    void convertAcceptedQuotationSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(quotationRepository.findByIdAndUserId(quotationId, userId)).thenReturn(Optional.of(mockQuotation));
        when(invoiceRepository.findByQuotationIdAndUserId(quotationId, userId)).thenReturn(Optional.empty());

        InvoiceNumberService.InvoiceNumberResult numberResult = new InvoiceNumberService.InvoiceNumberResult(
                "GKE-2627-0001", "2026-2027", 1, 7, "Terms", "Notes", "upi@bank"
        );
        when(invoiceNumberService.generateNextInvoiceNumber(eq(userId), any(LocalDate.class))).thenReturn(numberResult);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quotationItemRepository.findByQuotationIdAndUserIdAndIsDeletedFalse(quotationId, userId))
                .thenReturn(List.of(mockItem1, mockItem2));
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = quotationService.convertQuotationToInvoice(quotationId);

        assertThat(result).isNotNull();
        assertThat(result.getQuotation().getId()).isEqualTo(quotationId);
        assertThat(result.getInvoiceNumber()).isEqualTo("GKE-2627-0001");
        assertThat(result.getTotalAmount()).isEqualTo(300.0);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(mockQuotation.getStatus()).isEqualTo(QuotationStatus.CONVERTED);

        assertThat(result.getItems()).hasSize(2);

        // Check structured item mapping
        InvoiceItem item1 = result.getItems().get(0);
        assertThat(item1.getDescription()).isEqualTo("Custom Steel Plate");
        assertThat(item1.getDimension()).isEqualTo("10x20 ft");
        assertThat(item1.getKgs()).isEqualTo(50.5);
        assertThat(item1.getQuantity()).isEqualTo(2);
        assertThat(item1.getAmount()).isEqualTo(200.0);
        assertThat(item1.getRate()).isEqualTo(100.0); // 200 / 2

        InvoiceItem item2 = result.getItems().get(1);
        assertThat(item2.getDescription()).isEqualTo("Standard Labor");
        assertThat(item2.getDimension()).isNull();
        assertThat(item2.getKgs()).isNull();
        assertThat(item2.getQuantity()).isEqualTo(1);
        assertThat(item2.getAmount()).isEqualTo(100.0);
        assertThat(item2.getRate()).isEqualTo(100.0); // 100 / 1
    }

    @Test
    void convertDraftQuotationFails() {
        mockQuotation.setStatus(QuotationStatus.DRAFT);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(quotationRepository.findByIdAndUserId(quotationId, userId)).thenReturn(Optional.of(mockQuotation));
        when(invoiceRepository.findByQuotationIdAndUserId(quotationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.convertQuotationToInvoice(quotationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only ACCEPTED quotations can be converted");
    }

    @Test
    void convertDuplicateQuotationReturnsExistingInvoice() {
        Invoice existingInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("GKE-2627-0001")
                .user(mockUser)
                .client(mockClient)
                .quotation(mockQuotation)
                .totalAmount(300.0)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(quotationRepository.findByIdAndUserId(quotationId, userId)).thenReturn(Optional.of(mockQuotation));
        when(invoiceRepository.findByQuotationIdAndUserId(quotationId, userId)).thenReturn(Optional.of(existingInvoice));

        Invoice result = quotationService.convertQuotationToInvoice(quotationId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingInvoice.getId());
        assertThat(result.getInvoiceNumber()).isEqualTo("GKE-2627-0001");
        verify(invoiceNumberService, never()).generateNextInvoiceNumber(any(), any());
    }

    @Test
    void convertOtherUsersQuotationThrowsNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(2L); // Different user
        when(quotationRepository.findByIdAndUserId(quotationId, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.convertQuotationToInvoice(quotationId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Quotation not found");
    }

    @Test
    void convertQuotationZeroQuantityItemHandlesRateSafely() {
        mockItem1.setQuantity(0);
        mockItem1.setAmount(150.0);

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(quotationRepository.findByIdAndUserId(quotationId, userId)).thenReturn(Optional.of(mockQuotation));
        when(invoiceRepository.findByQuotationIdAndUserId(quotationId, userId)).thenReturn(Optional.empty());

        InvoiceNumberService.InvoiceNumberResult numberResult = new InvoiceNumberService.InvoiceNumberResult(
                "GKE-2627-0002", "2026-2027", 2, 7, "Terms", "Notes", "upi@bank"
        );
        when(invoiceNumberService.generateNextInvoiceNumber(eq(userId), any(LocalDate.class))).thenReturn(numberResult);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quotationItemRepository.findByQuotationIdAndUserIdAndIsDeletedFalse(quotationId, userId))
                .thenReturn(List.of(mockItem1));
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = quotationService.convertQuotationToInvoice(quotationId);

        assertThat(result.getItems().get(0).getRate()).isEqualTo(150.0); // Does not throw divide-by-zero or NaN
    }
}
