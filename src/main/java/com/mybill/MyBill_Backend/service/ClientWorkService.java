package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ClientSummaryProjection;
import com.mybill.MyBill_Backend.dto.ClientWorkDTO;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientWorkService {

    private final ClientWorkRepository workRepository;
    private final ClientRepository clientRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<ClientWorkDTO> getClientWork(UUID clientId) {
        Long userId = securityUtils.getCurrentUserId();

        clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));

        return workRepository.findByClientIdAndUserIdAndIsDeletedFalse(clientId, userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientWorkDTO> getAllWork() {
        Long userId = securityUtils.getCurrentUserId();

        return workRepository.findAllByUserIdOrderByDateDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientWorkDTO> getUnbilledWorks(UUID clientId) {
        Long userId = securityUtils.getCurrentUserId();

        clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));

        return workRepository
                .findByClientIdAndBilledFalseAndUserIdAndIsDeletedFalse(clientId, userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ClientWork addWork(UUID clientId, ClientWork work) {
        Long userId = securityUtils.getCurrentUserId();

        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));

        LocalDateTime now = LocalDateTime.now();

        if (work.getId() == null) {
            work.setId(UUID.randomUUID());
        }

        Integer quantity = work.getQuantity() != null ? work.getQuantity() : 1;
        Double rate = work.getRate() != null ? work.getRate() : 0.0;

        work.setQuantity(quantity);
        work.setRate(rate);
        work.setAmount(calculateAmount(rate, quantity));

        work.setClient(client);
        work.setUser(securityUtils.getCurrentUser());
        work.setDate(work.getDate() != null ? work.getDate() : now);
        work.setCreatedAt(work.getCreatedAt() != null ? work.getCreatedAt() : now);
        work.setUpdatedAt(now);
        work.setBilled(work.getBilled() != null ? work.getBilled() : false);
        work.setIsDeleted(work.getIsDeleted() != null ? work.getIsDeleted() : false);
        work.setVersion(work.getVersion() != null ? work.getVersion() : 1);

        return workRepository.save(work);
    }

    public ClientWork updateWork(UUID workId, ClientWork updatedWork) {
        Long userId = securityUtils.getCurrentUserId();

        ClientWork existing = workRepository.findByIdAndUserId(workId, userId)
                .orElseThrow(() -> new NotFoundException("Work not found or access denied"));

        Integer quantity = updatedWork.getQuantity() != null
                ? updatedWork.getQuantity()
                : existing.getQuantity();

        Double rate = updatedWork.getRate() != null
                ? updatedWork.getRate()
                : existing.getRate();

        existing.setDescription(updatedWork.getDescription());
        existing.setRate(rate);
        existing.setQuantity(quantity);
        existing.setAmount(calculateAmount(rate, quantity));
        existing.setDeviceId(updatedWork.getDeviceId());
        existing.setUpdatedAt(LocalDateTime.now());

        if (updatedWork.getDate() != null) {
            existing.setDate(updatedWork.getDate());
        }

        return workRepository.save(existing);
    }

    public void deleteWork(UUID workId) {
        Long userId = securityUtils.getCurrentUserId();

        ClientWork existing = workRepository.findByIdAndUserId(workId, userId)
                .orElseThrow(() -> new NotFoundException("Work not found or access denied"));

        existing.markDeleted(LocalDateTime.now());

        workRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Double getTotalAmount(UUID clientId) {
        Long userId = securityUtils.getCurrentUserId();

        Double total = workRepository.getTotalAmountByClientAndUserId(clientId, userId);

        return total == null ? 0.0 : total;
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryProjection> getClientSummary() {
        return workRepository.getClientSummaryForUser(securityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClientWorkDTO> getWorkUpdatedSince(LocalDateTime since, org.springframework.data.domain.Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();

        return workRepository.findByUserIdAndUpdatedAtAfter(userId, since, pageable)
                .map(this::convertToDTO);
    }

    private Double calculateAmount(Double rate, Integer quantity) {
        double safeRate = rate != null ? rate : 0.0;
        int safeQuantity = quantity != null ? quantity : 1;

        return safeRate * safeQuantity;
    }

    private ClientWorkDTO convertToDTO(ClientWork work) {
        var lastInvoiceItem = invoiceItemRepository
                .findTopByWorkIdAndUserIdAndIsDeletedFalseOrderByInvoiceInvoiceDateDescCreatedAtDesc(
                        work.getId(),
                        work.getUser() != null ? work.getUser().getId() : securityUtils.getCurrentUserId()
                )
                .orElse(null);

        return ClientWorkDTO.builder()
                .id(work.getId())
                .description(work.getDescription())
                .rate(work.getRate())
                .quantity(work.getQuantity())
                .amount(work.getAmount())
                .workDate(work.getDate())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .deletedAt(work.getDeletedAt())
                .clientId(work.getClient() != null ? work.getClient().getId() : null)
                .clientName(work.getClient() != null ? work.getClient().getName() : null)
                .billed(work.getBilled())
                .isDeleted(work.getIsDeleted())
                .invoiceId(lastInvoiceItem != null && lastInvoiceItem.getInvoice() != null
                        ? lastInvoiceItem.getInvoice().getId()
                        : work.getInvoice() != null ? work.getInvoice().getId() : null)
                .previousInvoiceNumber(lastInvoiceItem != null && lastInvoiceItem.getInvoice() != null
                        ? lastInvoiceItem.getInvoice().getInvoiceNumber()
                        : null)
                .lastBilledDate(lastInvoiceItem != null && lastInvoiceItem.getInvoice() != null
                        ? lastInvoiceItem.getInvoice().getInvoiceDate()
                        : null)
                .build();
    }
}
