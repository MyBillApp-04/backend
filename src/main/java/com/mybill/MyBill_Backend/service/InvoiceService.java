package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.InvoiceFilterDTO;
import com.mybill.MyBill_Backend.dto.InvoicePreview;
import com.mybill.MyBill_Backend.dto.InvoiceProjection;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.InvoiceItem;
import com.mybill.MyBill_Backend.entity.PaymentMode;
import com.mybill.MyBill_Backend.entity.PaymentStatus;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceItemRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.mybill.MyBill_Backend.event.InvoiceCreatedEvent;
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

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Invoice generateInvoice(
            UUID clientId,
            List<UUID> workIds,
            Double discount,
            String notes,
            LocalDateTime dueDate
    ) {
        Long userId = securityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Client client = clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        List<ClientWork> works = workRepository.findAllById(workIds);

        if (works.isEmpty() || works.size() != workIds.size()) {
            throw new RuntimeException("Some selected work records were not found");
        }

        for (ClientWork work : works) {
            if (work.getClient() == null || !work.getClient().getId().equals(clientId)) {
                throw new RuntimeException("Work " + work.getId() + " does not belong to client " + clientId);
            }

            if (work.getUser() == null || !work.getUser().getId().equals(userId)) {
                throw new RuntimeException("Work " + work.getId() + " does not belong to current user");
            }

            if (Boolean.TRUE.equals(work.getIsDeleted())) {
                throw new RuntimeException("Work " + work.getId() + " is deleted");
            }

            if (Boolean.TRUE.equals(work.getBilled())
                    || invoiceItemRepository.existsByWorkIdAndUserIdAndIsDeletedFalse(work.getId(), userId)) {
                throw new RuntimeException("Work " + work.getId() + " has already been billed");
            }
        }

        double subtotal = works.stream()
                .mapToDouble(work -> work.getAmount() != null ? work.getAmount() : 0.0)
                .sum();

        double finalDiscount = discount != null ? discount : 0.0;
        double grossAmount = subtotal - finalDiscount;

        if (grossAmount < 0) {
            throw new RuntimeException("Discount cannot be greater than subtotal");
        }

        double availableAdvance = clientFinancialService.getAdvanceBalance(clientId, userId);
        double advanceApplied = Math.min(availableAdvance, grossAmount);
        double netPayable = Math.max(grossAmount - advanceApplied, 0.0);

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
                .toList();

        invoice.setItems(items);

        works.forEach(work -> {
            work.setBilled(true);
            work.setInvoice(invoice);
            work.setUpdatedAt(now);
        });

        workRepository.saveAll(works);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        clientFinancialService.recordInvoiceCreated(savedInvoice, now);
        if (advanceApplied > 0) {
            clientFinancialService.applyAdvanceToInvoice(savedInvoice, advanceApplied, now);
        }

        eventPublisher.publishEvent(new InvoiceCreatedEvent(this, savedInvoice));

        return savedInvoice;
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Invoice updatePayment(
            UUID invoiceId,
            Double paidAmount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = getInvoiceById(invoiceId);
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
            throw new RuntimeException("Paid amount cannot be negative");
        }

        double cappedPaidAmount = Math.min(safePaidAmount, totalAmount);

        invoice.setPaidAmount(cappedPaidAmount);
        invoice.setPaymentMode(mode);
        invoice.setPaymentDate(paymentDate != null ? paymentDate : LocalDateTime.now());

        double pending = totalAmount - cappedPaidAmount;
        invoice.setPendingAmount(Math.max(pending, 0.0));
        invoice.setRemainingAmount(Math.max(pending, 0.0));

        if (totalAmount <= 0 || cappedPaidAmount >= totalAmount) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
        } else if (cappedPaidAmount > 0) {
            invoice.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            invoice.setPaymentStatus(PaymentStatus.UNPAID);
        }

        invoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Invoice updatePaymentForUser(
            UUID invoiceId,
            Long userId,
            Double paidAmount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found or access denied"));

        return applyPaymentUpdate(invoice, paidAmount, mode, paymentDate);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Invoice addPaymentForUser(
            UUID invoiceId,
            Long userId,
            Double amount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found or access denied"));

        double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        double safeAmount = amount != null ? amount : 0.0;

        if (safeAmount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        return applyPaymentUpdate(invoice, currentPaid + safeAmount, mode, paymentDate);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Invoice subtractPaymentForUser(
            UUID invoiceId,
            Long userId,
            Double amount,
            PaymentMode mode,
            LocalDateTime paymentDate
    ) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found or access denied"));

        double currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : 0.0;
        double safeAmount = amount != null ? amount : 0.0;

        if (safeAmount <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        return applyPaymentUpdate(invoice, Math.max(currentPaid - safeAmount, 0.0), mode, paymentDate);
    }

    @Transactional(readOnly = true)
    public InvoicePreview previewInvoice(UUID clientId, List<UUID> workIds) {
        Long userId = securityUtils.getCurrentUserId();

        InvoiceValidationResult result = validateInvoiceInput(clientId, workIds, userId);

        return new InvoicePreview(
                result.client(),
                result.works(),
                result.total()
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
                .orElseThrow(() -> new RuntimeException("Client not found or access denied"));

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
                .orElseThrow(() -> new RuntimeException("Invoice not found or access denied"));
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
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<Invoice> searchInvoices(
            String clientName,
            Integer month,
            Integer year
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String safeClientName = clientName == null ? "" : clientName.trim();

        return invoiceRepository.searchInvoices(
                userId,
                safeClientName,
                month,
                year
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
                pageable
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
                    pageable
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
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoices(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceProjection> getInvoicesProjected(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return invoiceRepository.findProjectedByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesUpdatedSince(LocalDateTime since) {
        Long userId = securityUtils.getCurrentUserId();

        return invoiceRepository.findByUserIdAndUpdatedAtAfter(
                userId,
                since
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
    }
}
