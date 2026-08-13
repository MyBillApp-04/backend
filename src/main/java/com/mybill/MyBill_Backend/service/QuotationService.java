package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.QuotationDTO;
import com.mybill.MyBill_Backend.dto.QuotationItemDTO;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.QuotationItemRepository;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final ClientRepository clientRepository;
    private final SecurityUtils securityUtils;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Quotation> getQuotationsForUser(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return quotationRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Quotation getQuotationById(UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return quotationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
    }

    @Transactional
    public Quotation createQuotation(QuotationDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        User currentUser = securityUtils.getCurrentUser();

        Client client = clientRepository.findByIdAndUserId(dto.getClientId(), userId)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        LocalDateTime issueTime = dto.getIssueDate() != null ? dto.getIssueDate() : LocalDateTime.now();

        // Server-side number assignment
        String quotationNumber = generateNextQuotationNumber(userId, issueTime);

        Quotation quotation = Quotation.builder()
                .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
                .user(currentUser)
                .client(client)
                .quotationNumber(quotationNumber)
                .status(dto.getStatus() != null ? dto.getStatus() : QuotationStatus.DRAFT)
                .issueDate(issueTime)
                .validUntilDate(dto.getValidUntilDate())
                .notes(dto.getNotes())
                .termsAndConditions(dto.getTermsAndConditions())
                .pdfUrl(dto.getPdfUrl())
                .pdfPath(dto.getPdfPath())
                .subtotal(roundMoney(dto.getSubtotal() != null ? dto.getSubtotal() : 0.0))
                .discount(roundMoney(dto.getDiscount() != null ? dto.getDiscount() : 0.0))
                .grossAmount(roundMoney(dto.getGrossAmount() != null ? dto.getGrossAmount() : 0.0))
                .totalAmount(roundMoney(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0))
                .netPayable(roundMoney(dto.getNetPayable() != null ? dto.getNetPayable() : 0.0))
                .isDeleted(false)
                .version(1)
                .build();

        // Public links are capability tokens, never predictable quotation identifiers.
        String tokenHash = QuotationPublicResponseService.hashToken(QuotationPublicResponseService.generateRandomToken());
        quotation.setPublicTokenHash(tokenHash);
        quotation.setTokenCreatedAt(LocalDateTime.now());
        quotation.setTokenExpiresAt(dto.getValidUntilDate() != null ? dto.getValidUntilDate() : LocalDateTime.now().plusDays(30));

        Quotation saved = quotationRepository.save(quotation);

        if (dto.getItems() != null) {
            List<QuotationItem> items = new ArrayList<>();
            for (QuotationItemDTO itemDto : dto.getItems()) {
                QuotationItem item = QuotationItem.builder()
                        .id(itemDto.getId() != null ? itemDto.getId() : UUID.randomUUID())
                        .quotation(saved)
                        .user(currentUser)
                        .description(itemDto.getDescription())
                        .dimension(itemDto.getDimension())
                        .quantity(itemDto.getQuantity() != null ? itemDto.getQuantity() : 1)
                        .kgs(itemDto.getKgs())
                        .amount(itemDto.getAmount() != null ? itemDto.getAmount() : 0.0)
                        .isDeleted(false)
                        .version(1)
                        .build();
                items.add(quotationItemRepository.save(item));
            }
            saved.setItems(items);
        }

        return saved;
    }

    @Transactional
    public Quotation updateQuotation(UUID id, QuotationDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        Quotation quotation = quotationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));

        if (dto.getClientId() != null && !dto.getClientId().equals(quotation.getClient().getId())) {
            Client client = clientRepository.findByIdAndUserId(dto.getClientId(), userId)
                    .orElseThrow(() -> new NotFoundException("Client not found"));
            quotation.setClient(client);
        }

        if (dto.getStatus() != null) {
            quotation.setStatus(dto.getStatus());
        }
        if (dto.getIssueDate() != null) {
            quotation.setIssueDate(dto.getIssueDate());
        }
        if (dto.getValidUntilDate() != null) {
            quotation.setValidUntilDate(dto.getValidUntilDate());
        }
        quotation.setNotes(dto.getNotes());
        quotation.setTermsAndConditions(dto.getTermsAndConditions());
        quotation.setPdfUrl(dto.getPdfUrl());
        quotation.setPdfPath(dto.getPdfPath());

        quotation.setSubtotal(dto.getSubtotal() != null ? dto.getSubtotal() : quotation.getSubtotal());
        quotation.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : quotation.getDiscount());
        quotation.setGrossAmount(dto.getGrossAmount() != null ? dto.getGrossAmount() : quotation.getGrossAmount());
        quotation.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : quotation.getTotalAmount());
        quotation.setNetPayable(dto.getNetPayable() != null ? dto.getNetPayable() : quotation.getNetPayable());

        // Update items
        if (dto.getItems() != null) {
            // Soft delete all existing items first
            List<QuotationItem> existingItems = quotationItemRepository.findByQuotationIdAndUserIdAndIsDeletedFalse(id, userId);
            for (QuotationItem existing : existingItems) {
                existing.markDeleted(LocalDateTime.now());
                quotationItemRepository.save(existing);
            }

            List<QuotationItem> newItems = new ArrayList<>();
            for (QuotationItemDTO itemDto : dto.getItems()) {
                QuotationItem item = QuotationItem.builder()
                        .id(itemDto.getId() != null ? itemDto.getId() : UUID.randomUUID())
                        .quotation(quotation)
                        .user(securityUtils.getCurrentUser())
                        .description(itemDto.getDescription())
                        .dimension(itemDto.getDimension())
                        .quantity(itemDto.getQuantity() != null ? itemDto.getQuantity() : 1)
                        .kgs(itemDto.getKgs())
                        .amount(itemDto.getAmount() != null ? itemDto.getAmount() : 0.0)
                        .isDeleted(false)
                        .version(1)
                        .build();
                newItems.add(quotationItemRepository.save(item));
            }
            quotation.setItems(newItems);
        }

        return quotationRepository.save(quotation);
    }

    @Transactional
    public void deleteQuotation(UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        Quotation quotation = quotationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));

        LocalDateTime now = LocalDateTime.now();
        quotation.markDeleted(now);
        quotationRepository.save(quotation);

        List<QuotationItem> items = quotationItemRepository.findByQuotationIdAndUserIdAndIsDeletedFalse(id, userId);
        for (QuotationItem item : items) {
            item.markDeleted(now);
            quotationItemRepository.save(item);
        }
    }

    @Transactional
    public Invoice convertQuotationToInvoice(UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();

        Quotation quotation = quotationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));

        if (Boolean.TRUE.equals(quotation.getIsDeleted())) {
            throw new NotFoundException("Quotation not found");
        }

        // Idempotency check: if invoice already exists for this quotation, return it
        Optional<Invoice> existingInvoice = invoiceRepository.findByQuotationIdAndUserId(id, userId);
        if (existingInvoice.isPresent()) {
            if (quotation.getStatus() != QuotationStatus.CONVERTED) {
                quotation.setStatus(QuotationStatus.CONVERTED);
                quotationRepository.save(quotation);
            }
            return existingInvoice.get();
        }

        // Quotations must be ACCEPTED (or already CONVERTED) to be converted
        if (quotation.getStatus() != QuotationStatus.ACCEPTED && quotation.getStatus() != QuotationStatus.CONVERTED) {
            throw new IllegalStateException("Only ACCEPTED quotations can be converted to an invoice. Current status: " + quotation.getStatus());
        }

        InvoiceNumberService.InvoiceNumberResult numResult = invoiceNumberService.generateNextInvoiceNumber(userId, java.time.LocalDate.now());

        LocalDateTime now = LocalDateTime.now();
        Double totalAmount = roundMoney(quotation.getTotalAmount() != null ? quotation.getTotalAmount() : 0.0);

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .user(user)
                .client(quotation.getClient())
                .quotation(quotation)
                .invoiceNumber(numResult.invoiceNumber())
                .financialYear(numResult.financialYear())
                .sequenceNo(numResult.sequenceNo())
                .subtotal(roundMoney(quotation.getSubtotal() != null ? quotation.getSubtotal() : 0.0))
                .discount(roundMoney(quotation.getDiscount() != null ? quotation.getDiscount() : 0.0))
                .grossAmount(roundMoney(quotation.getGrossAmount() != null ? quotation.getGrossAmount() : 0.0))
                .advanceApplied(0.0)
                .netPayable(roundMoney(quotation.getNetPayable() != null ? quotation.getNetPayable() : 0.0))
                .totalAmount(totalAmount)
                .paidAmount(0.0)
                .pendingAmount(totalAmount)
                .remainingAmount(totalAmount)
                .paymentStatus(PaymentStatus.UNPAID)
                .notes(quotation.getNotes())
                .createdDate(now)
                .invoiceDate(now)
                .dueDate(now.plusDays(numResult.defaultDueDays()))
                .updatedAt(now)
                .isDeleted(false)
                .version(1)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<QuotationItem> qItems = quotationItemRepository.findByQuotationIdAndUserIdAndIsDeletedFalse(id, userId);
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (QuotationItem qi : qItems) {
            int qty = qi.getQuantity() != null ? qi.getQuantity() : 1;
            double amt = roundMoney(qi.getAmount() != null ? qi.getAmount() : 0.0);
            double rate = qty > 0 ? roundMoney(amt / qty) : amt;

            InvoiceItem ii = InvoiceItem.builder()
                    .id(UUID.randomUUID())
                    .invoice(savedInvoice)
                    .user(user)
                    .work(null)
                    .description(qi.getDescription())
                    .dimension(qi.getDimension())
                    .kgs(qi.getKgs())
                    .quantity(qty)
                    .rate(rate)
                    .amount(amt)
                    .createdAt(now)
                    .updatedAt(now)
                    .isDeleted(false)
                    .version(1)
                    .build();
            invoiceItems.add(invoiceItemRepository.save(ii));
        }

        savedInvoice.setItems(invoiceItems);

        quotation.setStatus(QuotationStatus.CONVERTED);
        quotationRepository.save(quotation);

        return savedInvoice;
    }

    @Transactional
    public String generateNextQuotationNumber(Long userId, LocalDateTime issueDate) {
        LocalDateTime date = issueDate != null ? issueDate : LocalDateTime.now();
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        int endYear = startYear + 1;
        String financialYearLabel = startYear + "-" + endYear;
        String financialYearCode = String.format("%02d%02d", startYear % 100, endYear % 100);

        Object value = entityManager.createNativeQuery("""
                INSERT INTO public.quotation_financial_year_sequence (user_id, financial_year, last_sequence)
                VALUES (:userId, :financialYear, 1)
                ON CONFLICT (user_id, financial_year) DO UPDATE
                    SET last_sequence = public.quotation_financial_year_sequence.last_sequence + 1
                RETURNING last_sequence
                """)
                .setParameter("userId", userId)
                .setParameter("financialYear", financialYearLabel)
                .getSingleResult();
        int sequence = ((Number) value).intValue();
        return "QT-" + financialYearCode + "-" + String.format("%04d", sequence);
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
    }
}
