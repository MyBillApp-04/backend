package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.InvoiceFilterDTO;
import com.mybill.MyBill_Backend.dto.InvoicePreview;
import com.mybill.MyBill_Backend.dto.InvoiceProjection;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.InvoiceItem;
import com.mybill.MyBill_Backend.entity.LedgerEntryType;
import com.mybill.MyBill_Backend.entity.LedgerDirection;
import com.mybill.MyBill_Backend.entity.PaymentMode;
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
import com.mybill.MyBill_Backend.exception.ConflictException;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.mybill.MyBill_Backend.event.InvoiceCreatedEvent;
import com.mybill.MyBill_Backend.event.InvoiceUpdatedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ClientWorkRepository workRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final ClientFinancialService clientFinancialService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditTrailService auditTrailService;
    private final com.mybill.MyBill_Backend.observability.AppMetrics appMetrics;
    private final BusinessProfileRepository businessProfileRepository;

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice generateInvoice(
            UUID clientId,
            List<UUID> workIds,
            Double discount,
            String notes,
            LocalDateTime dueDate,
            Double taxRate,
            TaxType gstType
    ) {
        Long userId = securityUtils.getCurrentUserId();
        return generateInvoiceForUser(clientId, workIds, discount, notes, dueDate, taxRate, gstType, userId);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice generateInvoiceForUser(
            UUID clientId,
            List<UUID> workIds,
            Double discount,
            String notes,
            LocalDateTime dueDate,
            Double taxRate,
            TaxType gstType,
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Client client = clientRepository.findByIdAndUserIdAndIsDeletedFalseWithLock(clientId, userId)
                .orElseThrow(() -> new NotFoundException("Client not found"));

        List<ClientWork> works = workRepository.findAllById(workIds);

        if (works.isEmpty() || works.size() != workIds.size()) {
            throw new NotFoundException("Some selected work records were not found");
        }

        for (ClientWork work : works) {
            if (work.getClient() == null || !work.getClient().getId().equals(clientId)) {
                throw new ForbiddenException("Work " + work.getId() + " does not belong to client " + clientId);
            }

            if (work.getUser() == null || !work.getUser().getId().equals(userId)) {
                throw new ForbiddenException("Work " + work.getId() + " does not belong to current user");
            }

            if (Boolean.TRUE.equals(work.getIsDeleted())) {
                throw new NotFoundException("Work " + work.getId() + " is deleted");
            }

            if (Boolean.TRUE.equals(work.getBilled())
                    || invoiceItemRepository.existsByWorkIdAndUserIdAndIsDeletedFalse(work.getId(), userId)) {
                throw new ConflictException("Work " + work.getId() + " has already been billed");
            }
        }

        double subtotal = roundMoney(works.stream()
                .mapToDouble(work -> work.getAmount() != null ? work.getAmount() : 0.0)
                .sum());

        double finalDiscount = roundMoney(discount != null ? discount : 0.0);
        double grossAmount = roundMoney(subtotal - finalDiscount);

        if (grossAmount < 0) {
            throw new RuntimeException("Discount cannot be greater than subtotal");
        }

        // Authoritative GST snapshot (single authoritative tax path; mirrored offline).
        double safeRate = roundMoney(taxRate != null ? taxRate : 0.0);
        TaxType resolvedType = gstType;
        if (resolvedType == null) {
            String businessState = businessProfileRepository.findByUserId(userId)
                    .map(com.mybill.MyBill_Backend.entity.BusinessProfile::getState)
                    .orElse(null);
            resolvedType = safeRate > 0.0
                    ? TaxCalculator.resolveTaxType(businessState, client.getState())
                    : TaxType.NONE;
        }
        TaxCalculator.TaxBreakdown tax = TaxCalculator.calculate(grossAmount, safeRate, resolvedType);

        double availableAdvance = roundMoney(clientFinancialService.getAdvanceBalance(clientId, userId));
        double advanceApplied = roundMoney(Math.min(availableAdvance, tax.total));
        double netPayable = TaxCalculator.amountDue(tax.total, advanceApplied);

        LocalDateTime now = LocalDateTime.now();
        InvoiceNumberService.InvoiceNumberResult invoiceNumber =
                invoiceNumberService.generateNextInvoiceNumber(userId, now.toLocalDate());

        String finalNotes = (notes != null && !notes.isBlank()) ? notes : invoiceNumber.paymentNote();

        Invoice invoice = Invoice.builder()
                .client(client)
                .user(user)
                .invoiceNumber(invoiceNumber.invoiceNumber())
                .financialYear(invoiceNumber.financialYear())
                .sequenceNo(invoiceNumber.sequenceNo())
                .subtotal(subtotal)
                .discount(finalDiscount)
                .grossAmount(grossAmount)
                .taxRate(tax.rate)
                .taxType(tax.type)
                .taxableAmount(tax.taxableAmount)
                .taxAmount(tax.taxAmount)
                .cgstAmount(tax.cgstAmount)
                .sgstAmount(tax.sgstAmount)
                .igstAmount(tax.igstAmount)
                .total(tax.total)
                .advanceApplied(advanceApplied)
                .netPayable(netPayable)
                .totalAmount(netPayable)
                .paidAmount(0.0)
                .pendingAmount(netPayable)
                .remainingAmount(netPayable)
                .paymentStatus(netPayable <= 0 ? PaymentStatus.PAID : PaymentStatus.UNPAID)
                .invoiceDate(now)
                .dueDate(dueDate != null ? dueDate : now.plusDays(invoiceNumber.defaultDueDays()))
                .notes(finalNotes)
                .build();

        List<InvoiceItem> items = works.stream()
                .map(work -> InvoiceItem.builder()
                        .invoice(invoice)
                        .work(work)
                        .user(user)
                        .description(work.getDescription())
                        .rate(work.getRate())
                        .quantity(work.getQuantity())
                        .amount(work.getAmount())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        invoice.setItems(items);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        works.forEach(work -> {
            work.setBilled(true);
            work.setInvoice(savedInvoice);
            work.setUpdatedAt(now);
        });

        workRepository.saveAll(works);
        appMetrics.getInvoicesGenerated().increment();
        auditTrailService.logChange("Invoice", savedInvoice.getId(), "CREATE", "Invoice generated with total " + savedInvoice.getTotalAmount());
        clientFinancialService.recordInvoiceCreated(savedInvoice, now);
        if (advanceApplied > 0) {
            clientFinancialService.applyAdvanceToInvoice(savedInvoice, advanceApplied, now);
        }

        eventPublisher.publishEvent(new InvoiceCreatedEvent(this, savedInvoice));

        return savedInvoice;
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice updatePayment(
            UUID invoiceId,
            Double paidAmount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Long userId = securityUtils.getCurrentUserId();
        Invoice invoice = invoiceRepository.findByIdAndUserIdWithLock(invoiceId, userId)
                .orElseThrow(() -> new NotFoundException("Invoice not found or access denied"));
        return applyPaymentUpdate(invoice, paidAmount, mode, paymentDate);
    }

    private Invoice applyPaymentUpdate(
            Invoice invoice,
            Double paidAmount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        double safePaidAmount = paidAmount != null ? paidAmount : 0.0;
        double totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;

        if (safePaidAmount < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative");
        }

        double cappedPaidAmount = Math.min(safePaidAmount, totalAmount);

        invoice.setPaidAmount(cappedPaidAmount);
        invoice.setPaymentMode(mode);
        invoice.setPaymentDate(paymentDate != null ? paymentDate : LocalDateTime.now());

        // Delegate pending/remaining/status derivation to the entity's single
        // source of truth (mirror of preUpdate.applyPendingAndStatus).
        invoice.applyPendingAndStatus();

        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        if (PaymentStatus.PAID.equals(saved.getPaymentStatus())) {
            appMetrics.getInvoicesFullyPaid().increment();
        }
        auditTrailService.logChange("Invoice", saved.getId(), "UPDATE", "Recorded payment: paid " + saved.getPaidAmount() + ", pending " + saved.getPendingAmount());
        eventPublisher.publishEvent(new InvoiceUpdatedEvent(this, saved));
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice updatePaymentForUser(
            UUID invoiceId,
            Long userId,
            Double paidAmount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserIdWithLock(invoiceId, userId)
                .orElseThrow(() -> new NotFoundException("Invoice not found or access denied"));

        return applyPaymentUpdate(invoice, paidAmount, mode, paymentDate);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice addPaymentForUser(
            UUID invoiceId,
            Long userId,
            Double amount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserIdWithLock(invoiceId, userId)
                .orElseThrow(() -> new NotFoundException("Invoice not found or access denied"));

        double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        double safeAmount = amount != null ? amount : 0.0;

        if (safeAmount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        return applyPaymentUpdate(invoice, currentPaid + safeAmount, mode, paymentDate);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "clientFinancialSummary"}, allEntries = true)
    public Invoice subtractPaymentForUser(
            UUID invoiceId,
            Long userId,
            Double amount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserIdWithLock(invoiceId, userId)
                .orElseThrow(() -> new NotFoundException("Invoice not found or access denied"));

        double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        double safeAmount = amount != null ? amount : 0.0;

        if (safeAmount <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        return applyPaymentUpdate(invoice, Math.max(currentPaid - safeAmount, 0.0), mode, paymentDate);
    }

    @Transactional(readOnly = true)
    public InvoicePreview previewInvoice(UUID clientId, List<UUID> workIds, Double discount, Double taxRate, TaxType gstType) {
        Long userId = securityUtils.getCurrentUserId();

        InvoiceValidationResult result = validateInvoiceInput(clientId, workIds, userId);

        double subtotal = roundMoney(result.works().stream()
                .mapToDouble(w -> w.getAmount() != null ? w.getAmount() : 0.0)
                .sum());
        double finalDiscount = roundMoney(discount != null ? discount : 0.0);
        double grossAmount = roundMoney(subtotal - finalDiscount);
        if (grossAmount < 0) {
            throw new RuntimeException("Discount cannot be greater than subtotal");
        }

        double safeRate = roundMoney(taxRate != null ? taxRate : 0.0);
        TaxType resolved = gstType;
        if (resolved == null && safeRate > 0.0) {
            String businessState = businessProfileRepository.findByUserId(userId)
                    .map(com.mybill.MyBill_Backend.entity.BusinessProfile::getState)
                    .orElse(null);
            resolved = TaxCalculator.resolveTaxType(businessState, result.client().getState());
        }
        if (resolved == null) {
            resolved = TaxType.NONE;
        }
        TaxCalculator.TaxBreakdown tax = TaxCalculator.calculate(grossAmount, safeRate, resolved);

        return new InvoicePreview(
                result.client(),
                result.works(),
                tax.total,
                tax.rate,
                tax.type,
                tax.taxableAmount,
                tax.taxAmount,
                tax.cgstAmount,
                tax.sgstAmount,
                tax.igstAmount
        );
    }

    private InvoiceValidationResult validateInvoiceInput(
            UUID clientId,
            List<UUID> workIds,
            Long userId
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID is required");
        }

        if (workIds == null || workIds.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one work item");
        }

        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new NotFoundException("Client not found or access denied"));

        List<ClientWork> works = workRepository.findAllById(workIds);

        if (works.isEmpty() || works.size() != workIds.size()) {
            throw new IllegalArgumentException("Some selected work records were not found");
        }

        boolean invalidWork = works.stream().anyMatch(work ->
                work.getClient() == null ||
                        work.getUser() == null ||
                        !work.getClient().getId().equals(clientId) ||
                        !work.getUser().getId().equals(userId) ||
                        Boolean.TRUE.equals(work.getIsDeleted())
        );

        if (invalidWork) {
            throw new IllegalArgumentException("Invalid work selection");
        }

        double total = works.stream()
                .mapToDouble(work -> work.getAmount() != null ? work.getAmount() : 0.0)
                .sum();

        if (total <= 0) {
            throw new IllegalArgumentException("Invalid total amount");
        }

        return new InvoiceValidationResult(client, works, total);
    }

    private record InvoiceValidationResult(
            Client client,
            List<ClientWork> works,
            double total
    ) {
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID id) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Invoice not found or access denied"));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByClient(UUID clientId) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByClientIdAndUserIdAndIsDeletedFalse(
                clientId,
                userId
        );
    }

    // NEW: Paginated projection fetch
    @Transactional(readOnly = true)
    public Page<InvoiceProjection> getInvoicesByClientProjected(UUID clientId, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findProjectedByClientIdAndUserIdAndIsDeletedFalse(
                clientId,
                userId,
                defaultInvoicePageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    public Page<Invoice> searchInvoices(
            String clientName,
            Integer month,
            Integer year,
            Pageable pageable
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String safeClientName = clientName == null ? "" : clientName.trim();

        return invoiceRepository.searchInvoices(
                userId,
                safeClientName,
                month,
                year,
                defaultInvoicePageable(pageable)
        );
    }

    // NEW: Paginated projection fetch
    @Transactional(readOnly = true)
    public Page<InvoiceProjection> searchInvoicesProjected(
            String clientName,
            Integer month,
            Integer year,
            Pageable pageable
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String safeClientName = clientName == null ? "" : clientName.trim();

        return invoiceRepository.searchProjectedInvoices(
                userId,
                safeClientName,
                month,
                year,
                defaultInvoicePageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    public Page<InvoiceProjection> filterInvoices(InvoiceFilterDTO filter, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        InvoiceFilterDTO safeFilter = filter != null ? filter : new InvoiceFilterDTO();
        String query = safeFilter.getQuery() == null ? "" : safeFilter.getQuery().trim();
        List<PaymentStatus> statuses = safeFilter.getStatuses();

        if (statuses != null && statuses.isEmpty()) {
            statuses = null;
        }

        if (statuses == null) {
            return invoiceRepository.filterInvoicesWithoutStatuses(
                    userId,
                    query,
                    safeFilter.getClientId(),
                    safeFilter.getStartDate(),
                    safeFilter.getEndDate(),
                    safeFilter.getMinAmount(),
                    safeFilter.getMaxAmount(),
                    defaultInvoicePageable(pageable)
            );
        }

        return invoiceRepository.filterInvoices(
                userId,
                query,
                safeFilter.getClientId(),
                safeFilter.getStartDate(),
                safeFilter.getEndDate(),
                statuses,
                safeFilter.getMinAmount(),
                safeFilter.getMaxAmount(),
                defaultInvoicePageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoices(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByUserIdAndIsDeletedFalse(userId, defaultInvoicePageable(pageable));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceProjection> getInvoicesProjected(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return invoiceRepository.findProjectedByUserIdAndIsDeletedFalse(userId, defaultInvoicePageable(pageable));
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesUpdatedSince(LocalDateTime since, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByUserIdAndUpdatedAtAfter(
                userId,
                since,
                defaultInvoicePageable(pageable)
        );
    }

    private Pageable defaultInvoicePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));
        }

        if (pageable.getSort().isSorted()) {
            return pageable;
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdDate")
        );
    }

    @Transactional(readOnly = true)
    public double getMonthlyRevenue() {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();

        Double revenue = invoiceRepository.sumTotalAmountByUserIdAndYearAndMonth(
                userId,
                now.getYear(),
                now.getMonthValue()
        );

        return revenue != null ? revenue : 0.0;
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteInvoice(UUID invoiceId) {
        Long userId = securityUtils.getCurrentUserId();

        Invoice invoice = getInvoiceById(invoiceId);
        LocalDateTime now = LocalDateTime.now();

        double advanceApplied = invoice.getAdvanceApplied() != null ? invoice.getAdvanceApplied() : 0.0;
        UUID clientId = invoice.getClient().getId();

        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            List<UUID> workIds = invoice.getItems()
                    .stream()
                    .filter(item -> item.getWork() != null)
                    .map(item -> item.getWork().getId())
                    .toList();

            List<ClientWork> linkedWorks = workRepository.findAllById(workIds);

            linkedWorks.forEach(work -> {
                if (
                        work.getUser() != null &&
                                work.getUser().getId().equals(userId) &&
                                work.getInvoice() != null &&
                                work.getInvoice().getId().equals(invoiceId)
                ) {
                    work.setBilled(false);
                    work.setInvoice(null);
                    work.setUpdatedAt(now);
                }
            });

            workRepository.saveAll(linkedWorks);

            invoice.getItems().forEach(item -> item.markDeleted(now));

            invoiceItemRepository.saveAll(invoice.getItems());
        }

        invoice.markDeleted(now);

        invoiceRepository.save(invoice);
        auditTrailService.logChange("Invoice", invoice.getId(), "DELETE", "Soft deleted invoice");

        // Release advance back to customer balance if advance was applied
        if (advanceApplied > 0) {
            clientFinancialService.releaseAdvanceFromInvoice(clientId, userId, advanceApplied, now, invoice.getInvoiceNumber());
        }
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
