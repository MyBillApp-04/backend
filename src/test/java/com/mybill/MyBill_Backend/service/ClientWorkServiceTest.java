package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ClientWorkDTO;
import com.mybill.MyBill_Backend.dto.ClientWorkRequest;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClientWorkServiceTest {

    private ClientWorkRepository workRepository;
    private ClientRepository clientRepository;
    private InvoiceItemRepository invoiceItemRepository;
    private SecurityUtils securityUtils;
    private ClientWorkService service;

    private User testUser;
    private Client testClient;
    private UUID clientId;
    private UUID workId;

    @BeforeEach
    void setUp() {
        workRepository = mock(ClientWorkRepository.class);
        clientRepository = mock(ClientRepository.class);
        invoiceItemRepository = mock(InvoiceItemRepository.class);
        securityUtils = mock(SecurityUtils.class);

        service = new ClientWorkService(workRepository, clientRepository, invoiceItemRepository, securityUtils);

        testUser = User.builder().id(100L).email("user@example.com").build();
        clientId = UUID.randomUUID();
        workId = UUID.randomUUID();
        testClient = Client.builder().id(clientId).name("Test Client").user(testUser).build();

        when(securityUtils.getCurrentUserId()).thenReturn(100L);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void addWorkCalculatesAmountAndAssignsDefaults() {
        when(clientRepository.findByIdAndUserId(clientId, 100L)).thenReturn(Optional.of(testClient));
        when(workRepository.save(any(ClientWork.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientWorkRequest request = new ClientWorkRequest();
        request.setDescription("Design Website Homepage");
        request.setQuantity(5);
        request.setRate(150.0);
        request.setDeviceId("dev-1");

        ClientWork saved = service.addWork(clientId, request);

        assertThat(saved).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Design Website Homepage");
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getRate()).isEqualTo(150.0);
        assertThat(saved.getAmount()).isEqualTo(750.0);
        assertThat(saved.getClient()).isEqualTo(testClient);
        assertThat(saved.getBilled()).isFalse();
        assertThat(saved.getIsDeleted()).isFalse();
    }

    @Test
    void addWorkThrowsForbiddenExceptionWhenClientNotFound() {
        when(clientRepository.findByIdAndUserId(clientId, 100L)).thenReturn(Optional.empty());

        ClientWorkRequest request = new ClientWorkRequest();
        request.setDescription("Task");

        assertThatThrownBy(() -> service.addWork(clientId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void updateWorkUpdatesFieldsAndRecalculatesAmount() {
        ClientWork existing = new ClientWork();
        existing.setId(workId);
        existing.setUser(testUser);
        existing.setClient(testClient);
        existing.setRate(100.0);
        existing.setQuantity(2);
        existing.setAmount(200.0);

        when(workRepository.findByIdAndUserId(workId, 100L)).thenReturn(Optional.of(existing));
        when(workRepository.save(any(ClientWork.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientWorkRequest request = new ClientWorkRequest();
        request.setDescription("Updated Task");
        request.setQuantity(4);
        request.setRate(120.0);

        ClientWork updated = service.updateWork(workId, request);

        assertThat(updated.getDescription()).isEqualTo("Updated Task");
        assertThat(updated.getQuantity()).isEqualTo(4);
        assertThat(updated.getRate()).isEqualTo(120.0);
        assertThat(updated.getAmount()).isEqualTo(480.0);
    }

    @Test
    void deleteWorkMarksWorkAsDeleted() {
        ClientWork existing = new ClientWork();
        existing.setId(workId);
        existing.setUser(testUser);
        existing.setIsDeleted(false);

        when(workRepository.findByIdAndUserId(workId, 100L)).thenReturn(Optional.of(existing));

        service.deleteWork(workId);

        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(workRepository, times(1)).save(existing);
    }

    @Test
    void getClientWorkReturnsMappedDTOPage() {
        ClientWork work = new ClientWork();
        work.setId(workId);
        work.setDescription("Consulting");
        work.setRate(200.0);
        work.setQuantity(3);
        work.setAmount(600.0);
        work.setClient(testClient);
        work.setUser(testUser);

        when(clientRepository.findByIdAndUserId(clientId, 100L)).thenReturn(Optional.of(testClient));
        when(workRepository.findByClientIdAndUserIdAndIsDeletedFalse(eq(clientId), eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(work)));
        when(invoiceItemRepository.findTopByWorkIdAndUserIdAndIsDeletedFalseOrderByInvoiceInvoiceDateDescCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        Page<ClientWorkDTO> page = service.getClientWork(clientId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        ClientWorkDTO dto = page.getContent().get(0);
        assertThat(dto.getDescription()).isEqualTo("Consulting");
        assertThat(dto.getAmount()).isEqualTo(600.0);
        assertThat(dto.getClientName()).isEqualTo("Test Client");
    }

    @Test
    void getTotalAmountReturnsValueOrZeroIfNull() {
        when(workRepository.getTotalAmountByClientAndUserId(clientId, 100L)).thenReturn(1500.0);
        assertThat(service.getTotalAmount(clientId)).isEqualTo(1500.0);

        when(workRepository.getTotalAmountByClientAndUserId(clientId, 100L)).thenReturn(null);
        assertThat(service.getTotalAmount(clientId)).isEqualTo(0.0);
    }
}
