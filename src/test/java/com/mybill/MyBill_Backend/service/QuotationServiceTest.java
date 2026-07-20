package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.QuotationDTO;
import com.mybill.MyBill_Backend.dto.QuotationItemDTO;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.QuotationItemRepository;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationItemRepository quotationItemRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private QuotationService quotationService;

    private User mockUser;
    private Client mockClient;
    private Quotation mockQuotation;
    private UUID quotationId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        quotationId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        mockClient = Client.builder()
                .id(clientId)
                .name("Acme Corp")
                .user(mockUser)
                .build();

        mockQuotation = Quotation.builder()
                .id(quotationId)
                .user(mockUser)
                .client(mockClient)
                .quotationNumber("QT-2627-0001")
                .status(QuotationStatus.DRAFT)
                .issueDate(LocalDateTime.now())
                .subtotal(100.0)
                .totalAmount(100.0)
                .netPayable(100.0)
                .isDeleted(false)
                .version(1)
                .build();
    }

    @Test
    void getQuotationsForUserSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(quotationRepository.findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockQuotation)));

        Page<Quotation> result = quotationService.getQuotationsForUser(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getQuotationNumber()).isEqualTo("QT-2627-0001");
    }

    @Test
    void getQuotationByIdSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(quotationRepository.findByIdAndUserId(quotationId, 1L)).thenReturn(Optional.of(mockQuotation));

        Quotation result = quotationService.getQuotationById(quotationId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(quotationId);
    }

    @Test
    void createQuotationSuccess() {
        QuotationDTO dto = QuotationDTO.builder()
                .clientId(clientId)
                .subtotal(200.0)
                .totalAmount(200.0)
                .items(List.of(
                        QuotationItemDTO.builder()
                                .description("Item 1")
                                .quantity(2)
                                .amount(100.0)
                                .build()
                ))
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(clientRepository.findByIdAndUserId(clientId, 1L)).thenReturn(Optional.of(mockClient));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quotationItemRepository.save(any(QuotationItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock Native Query Sequence generator
        Query mockQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(5);

        Quotation created = quotationService.createQuotation(dto);

        assertThat(created).isNotNull();
        assertThat(created.getSubtotal()).isEqualTo(200.0);
        assertThat(created.getQuotationNumber()).startsWith("QT-");
        verify(quotationRepository, times(1)).save(any(Quotation.class));
    }

    @Test
    void updateQuotationSuccess() {
        QuotationDTO dto = QuotationDTO.builder()
                .status(QuotationStatus.SENT)
                .notes("Updated notes")
                .items(List.of())
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(quotationRepository.findByIdAndUserId(quotationId, 1L)).thenReturn(Optional.of(mockQuotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));

        Quotation updated = quotationService.updateQuotation(quotationId, dto);

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(QuotationStatus.SENT);
        assertThat(updated.getNotes()).isEqualTo("Updated notes");
    }

    @Test
    void deleteQuotationSuccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(quotationRepository.findByIdAndUserId(quotationId, 1L)).thenReturn(Optional.of(mockQuotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));

        quotationService.deleteQuotation(quotationId);

        assertThat(mockQuotation.getIsDeleted()).isTrue();
        assertThat(mockQuotation.getDeletedAt()).isNotNull();
    }
}
