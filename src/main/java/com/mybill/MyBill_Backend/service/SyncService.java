package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.sync.*;
import com.mybill.MyBill_Backend.dto.sync.payload.*;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SyncService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;

    private final ClientRepository clientRepository;
    private final ClientWorkRepository workRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ClientLedgerEntryRepository ledgerEntryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final SyncDeviceStateRepository syncDeviceStateRepository;
    private final PlatformTransactionManager transactionManager;
    private final InvoiceNumberService invoiceNumberService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public SyncResponse sync(SyncRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";

        try {
            return doSync(request);
        } catch (RuntimeException exception) {
            outcome = "error";
            throw exception;
        } finally {
            sample.stop(Timer.builder("mybill.sync.duration")
                    .description("Time spent processing sync requests")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private SyncResponse doSync(SyncRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();

        LocalDateTime serverTime = LocalDateTime.now();
        String conflictPolicy = request.getConflictPolicy() != null && !request.getConflictPolicy().isBlank()
                ? request.getConflictPolicy()
                : "SERVER_WINS";
        int conflictCount = 0;

        List<String> accepted = new ArrayList<>();
        List<RejectedChangeDto> rejected = new ArrayList<>();

        if (request.getChanges() != null) {
            for (SyncChangeDto change : orderedChanges(request.getChanges())) {
                try {
                    boolean applied = applyChangeInTransaction(change, user, userId, request.getDeviceId(), serverTime, conflictPolicy);
                    if (applied) {
                        accepted.add(change.getChangeId());
                    } else {
                        conflictCount++;
                        rejected.add(conflict(change, "Server version is newer; change rejected by conflict policy"));
                    }
                } catch (StackOverflowError e) {
                    rejected.add(conflict(change, "Server sync recursion while saving " + change.getEntityType()));
                } catch (Exception e) {
                    rejected.add(conflict(change, rootCauseMessage(e)));
                }
            }
        }

        int pageSize = normalizePageSize(request.getPageSize());
        String cursor = request.getCursor();

        PullCursor pullCursor = PullCursor.from(cursor);

        SyncPageResult pageResult = pullChanges(
                userId,
                request.getLastPulledAt(),
                pullCursor,
                pageSize
        );

        updateDeviceState(request, user, serverTime, conflictCount);

        return SyncResponse.builder()
                .serverTime(serverTime)
                .acceptedChangeIds(accepted)
                .rejected(rejected)
                .changes(pageResult.getChanges())
                .nextCursor(pageResult.getNextCursor())
                .hasMore(pageResult.isHasMore())
                .conflictPolicy(conflictPolicy)
                .conflictCount(conflictCount)
                .build();
    }

    private boolean applyChangeInTransaction(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

        return Boolean.TRUE.equals(template.execute(status -> {
            User managedUser = userRepository.getReferenceById(userId);
            boolean applied;
            switch (change.getEntityType()) {
                case "client" -> {
                    applied = applyClientChange(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
                }
                case "work" -> {
                    ClientWork w = buildWork(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
                    applied = w != null;
                    if (applied) workRepository.saveAndFlush(w);
                }
                case "invoice" -> {
                    boolean isNew = invoiceRepository.findByIdAndUserId(
                            UUID.fromString(change.getEntityId()), userId).isEmpty();
                    Invoice i = buildInvoice(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
                    applied = i != null;
                    if (applied) {
                        Invoice saved = invoiceRepository.saveAndFlush(i);
                        if (isNew && !"delete".equalsIgnoreCase(change.getOperation())) {
                            eventPublisher.publishEvent(
                                    new com.mybill.MyBill_Backend.event.InvoiceCreatedEvent(this, saved));
                        }
                    }
                }
                case "invoice_item" -> {
                    InvoiceItem ii = buildInvoiceItem(change, managedUser, userId, deviceId, serverTime);
                    applied = ii != null;
                    if (applied) invoiceItemRepository.saveAndFlush(ii);
                }
                case "ledger_entry" -> {
                    ClientLedgerEntry le = buildLedgerEntry(change, managedUser, userId, deviceId, serverTime);
                    applied = le != null;
                    if (applied) ledgerEntryRepository.saveAndFlush(le);
                }
                default -> throw new RuntimeException("Unsupported entity type: " + change.getEntityType());
            }
            return applied;
        }));
    }

    private boolean applyClientChange(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Client id missing");
        ClientSyncPayload payload = toPayload(change, ClientSyncPayload.class);
        Optional<Client> existing = clientRepository.findByIdAndUserId(id, userId);

        if (existing.isPresent()
                && hasServerConflict(existing.get().getUpdatedAt(), change.getCreatedAt(), conflictPolicy)) {
            return false;
        }

        String resolvedDeviceId = valueOrDefault(payload.getDeviceId(), deviceId);

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            if (existing.isPresent()) {
                clientRepository.markClientDeletedFromSync(
                        id,
                        userId,
                        serverTime,
                        resolvedDeviceId,
                        nextVersion(existing.get().getVersion())
                );
                return true;
            }

            Client client = new Client();
            client.setId(id);
            client.setUser(user);
            client.setName(upper(payload.getName()));
            client.setPhone(payload.getPhone());
            client.setEmail(payload.getEmail());
            client.setAddress(payload.getAddress());
            client.setDeviceId(resolvedDeviceId);
            client.setCreatedAt(serverTime);
            client.markDeleted(serverTime);
            client.setVersion(1);
            clientRepository.saveAndFlush(client);
            return true;
        }

        if (existing.isPresent()) {
            clientRepository.updateClientFromSync(
                    id,
                    userId,
                    upper(payload.getName()),
                    payload.getPhone(),
                    payload.getEmail(),
                    payload.getAddress(),
                    resolvedDeviceId,
                    serverTime,
                    payload.getDeletedAt(),
                    Boolean.TRUE.equals(payload.getIsDeleted()),
                    nextVersion(existing.get().getVersion())
            );
            return true;
        }

        Client client = new Client();
        client.setId(id);
        client.setUser(user);
        client.setName(upper(payload.getName()));
        client.setPhone(payload.getPhone());
        client.setEmail(payload.getEmail());
        client.setAddress(payload.getAddress());
        client.setDeviceId(resolvedDeviceId);
        client.setCreatedAt(serverTime);
        client.setUpdatedAt(serverTime);
        client.setDeletedAt(payload.getDeletedAt());
        client.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
        client.setVersion(1);
        clientRepository.saveAndFlush(client);
        return true;
    }

    private List<SyncChangeDto> orderedChanges(List<SyncChangeDto> changes) {
        return changes.stream()
                .sorted(Comparator.comparingInt(change -> switch (change.getEntityType()) {
                    case "client" -> 0;
                    case "work" -> 1;
                    case "invoice" -> 2;
                    case "invoice_item" -> 3;
                    case "ledger_entry" -> 4;
                    default -> 4;
                }))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDeviceSyncStatus(String deviceId) {
        Long userId = securityUtils.getCurrentUserId();
        return syncDeviceStateRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(state -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("deviceId", state.getDeviceId());
                    m.put("lastPulledAt", state.getLastPulledAt());
                    m.put("lastPushedAt", state.getLastPushedAt());
                    m.put("lastSeenAt", state.getLastSeenAt());
                    m.put("conflictCount", state.getConflictCount());
                    return m;
                })
                .orElseGet(() -> Map.of("deviceId", deviceId, "status", "UNKNOWN"));
    }

    private void updateDeviceState(SyncRequest request, User user, LocalDateTime serverTime, int conflictCount) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            return;
        }

        boolean pushed = request.getChanges() != null && !request.getChanges().isEmpty();
        int updated = syncDeviceStateRepository.updateSyncState(
                user.getId(), request.getDeviceId(), serverTime, pushed, conflictCount);
        if (updated > 0) {
            return;
        }

        SyncDeviceState state = SyncDeviceState.builder()
                .user(user)
                .deviceId(request.getDeviceId())
                .lastPulledAt(serverTime)
                .lastPushedAt(pushed ? serverTime : null)
                .lastSeenAt(serverTime)
                .conflictCount(conflictCount)
                .build();

        try {
            syncDeviceStateRepository.saveAndFlush(state);
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent sync created the state after our update. Touch it now.
            syncDeviceStateRepository.updateSyncState(
                    user.getId(), request.getDeviceId(), serverTime, pushed, conflictCount);
        }
    }

    private SyncPageResult pullChanges(
            Long userId,
            LocalDateTime since,
            PullCursor cursor,
            int pageSize
    ) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));

        // Clients
        EntityCursor cc = cursor.get("client");
        Page<Client> clientPage;
        if (cc != null && cc.time != null && cc.id != null) {
            clientPage = clientRepository.findByUserIdWithKeyset(userId, cc.time, cc.id, pageable);
        } else {
            clientPage = since == null ? clientRepository.findByUserId(userId, pageable) : clientRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!clientPage.isEmpty()) {
            Client last = clientPage.getContent().get(clientPage.getContent().size() - 1);
            cursor.put("client", last.getUpdatedAt(), last.getId(), clientPage.hasNext());
        } else {
            cursor.put("client", null, null, false);
        }

        // Works
        EntityCursor wc = cursor.get("work");
        Page<ClientWork> workPage;
        if (wc != null && wc.time != null && wc.id != null) {
            workPage = workRepository.findByUserIdWithKeyset(userId, wc.time, wc.id, pageable);
        } else {
            workPage = since == null ? workRepository.findByUserId(userId, pageable) : workRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!workPage.isEmpty()) {
            ClientWork last = workPage.getContent().get(workPage.getContent().size() - 1);
            cursor.put("work", last.getUpdatedAt(), last.getId(), workPage.hasNext());
        } else {
            cursor.put("work", null, null, false);
        }

        // Invoices
        EntityCursor ic = cursor.get("invoice");
        Page<Invoice> invoicePage;
        if (ic != null && ic.time != null && ic.id != null) {
            invoicePage = invoiceRepository.findByUserIdWithKeyset(userId, ic.time, ic.id, pageable);
        } else {
            invoicePage = since == null ? invoiceRepository.findByUserId(userId, pageable) : invoiceRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!invoicePage.isEmpty()) {
            Invoice last = invoicePage.getContent().get(invoicePage.getContent().size() - 1);
            cursor.put("invoice", last.getUpdatedAt(), last.getId(), invoicePage.hasNext());
        } else {
            cursor.put("invoice", null, null, false);
        }

        // Invoice Items
        EntityCursor iic = cursor.get("invoice_item");
        Page<InvoiceItem> itemPage;
        if (iic != null && iic.time != null && iic.id != null) {
            itemPage = invoiceItemRepository.findByUserIdWithKeyset(userId, iic.time, iic.id, pageable);
        } else {
            itemPage = since == null ? invoiceItemRepository.findByUserId(userId, pageable) : invoiceItemRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!itemPage.isEmpty()) {
            InvoiceItem last = itemPage.getContent().get(itemPage.getContent().size() - 1);
            cursor.put("invoice_item", last.getUpdatedAt(), last.getId(), itemPage.hasNext());
        } else {
            cursor.put("invoice_item", null, null, false);
        }

        // Ledger Entries
        EntityCursor lc = cursor.get("ledger_entry");
        Page<ClientLedgerEntry> ledgerPage;
        if (lc != null && lc.time != null && lc.id != null) {
            ledgerPage = ledgerEntryRepository.findByUserIdWithKeyset(userId, lc.time, lc.id, pageable);
        } else {
            ledgerPage = since == null ? ledgerEntryRepository.findByUserId(userId, pageable) : ledgerEntryRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!ledgerPage.isEmpty()) {
            ClientLedgerEntry last = ledgerPage.getContent().get(ledgerPage.getContent().size() - 1);
            cursor.put("ledger_entry", last.getUpdatedAt(), last.getId(), ledgerPage.hasNext());
        } else {
            cursor.put("ledger_entry", null, null, false);
        }

        changes.put("clients", clientPage.getContent().stream().map(this::clientToMap).toList());
        changes.put("works", workPage.getContent().stream().map(this::workToMap).toList());
        changes.put("invoices", invoicePage.getContent().stream().map(this::invoiceToMap).toList());
        changes.put("invoiceItems", itemPage.getContent().stream().map(this::invoiceItemToMap).toList());
        changes.put("ledgerEntries", ledgerPage.getContent().stream().map(this::ledgerEntryToMap).toList());

        boolean hasMore = cursor.hasMoreOverall();
        String nextCursor = hasMore ? cursor.encode() : null;

        return new SyncPageResult(changes, nextCursor, hasMore);
    }

    private Client buildClient(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Client id missing");
        ClientSyncPayload payload = toPayload(change, ClientSyncPayload.class);

        Client client = clientRepository.findByIdAndUserId(id, userId)
                .orElseGet(Client::new);

        if (hasServerConflict(client.getUpdatedAt(), change.getCreatedAt(), conflictPolicy)) {
            return null;
        }

        client.setId(id);
        client.setUser(user);
        client.setName(upper(payload.getName()));
        client.setPhone(payload.getPhone());
        client.setEmail(payload.getEmail());
        client.setAddress(payload.getAddress());
        client.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (client.getCreatedAt() == null) {
            client.setCreatedAt(serverTime);
        }

        client.setUpdatedAt(serverTime);
        client.setVersion(nextVersion(client.getVersion()));

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            client.markDeleted(serverTime);
        } else {
            client.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            client.setDeletedAt(payload.getDeletedAt());
        }

        return client;
    }

    private ClientWork buildWork(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Work id missing");
        WorkSyncPayload payload = toPayload(change, WorkSyncPayload.class);

        UUID clientId = requireUuid(payload.getClientId(), "Client id missing for work");
        requireText(payload.getDescription(), "Work description is required");
        requirePositive(payload.getRate(), "Work rate must be positive");
        requirePositive(payload.getQuantity(), "Work quantity must be positive");
        if (payload.getWorkDate() != null && payload.getWorkDate().isAfter(serverTime)) {
            throw new IllegalArgumentException("Work date cannot be in the future");
        }

        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new NotFoundException("Client not found for work"));

        ClientWork work = workRepository.findByIdAndUserId(id, userId)
                .orElseGet(ClientWork::new);

        if (hasServerConflict(work.getUpdatedAt(), change.getCreatedAt(), conflictPolicy)) {
            return null;
        }

        work.setId(id);
        work.setUser(user);
        work.setClient(client);
        work.setDescription(payload.getDescription());
        work.setRate(valueOrDefault(payload.getRate(), 0.0));
        work.setQuantity(valueOrDefault(payload.getQuantity(), 1));
        work.setAmount(valueOrDefault(payload.getAmount(), work.getRate() * work.getQuantity()));
        work.setDate(valueOrDefault(payload.getWorkDate(), serverTime));
        work.setBilled(Boolean.TRUE.equals(payload.getBilled()));
        work.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (payload.getInvoiceId() != null) {
            UUID invoiceId = requireUuid(payload.getInvoiceId(), "Invalid invoice id for work");
            invoiceRepository.findByIdAndUserId(invoiceId, userId)
                    .ifPresent(work::setInvoice);
        } else {
            work.setInvoice(null);
        }

        if (work.getCreatedAt() == null) {
            work.setCreatedAt(serverTime);
        }

        work.setUpdatedAt(serverTime);
        work.setVersion(nextVersion(work.getVersion()));

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            work.markDeleted(serverTime);
        } else {
            work.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            work.setDeletedAt(payload.getDeletedAt());
        }

        return work;
    }

    private Invoice buildInvoice(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Invoice id missing");
        InvoiceSyncPayload payload = toPayload(change, InvoiceSyncPayload.class);

        UUID clientId = requireUuid(payload.getClientId(), "Client id missing for invoice");

        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found for invoice"));

        Optional<Invoice> existingInvoice = invoiceRepository.findByIdAndUserId(id, userId);
        Invoice invoice = existingInvoice.orElseGet(Invoice::new);

        if (hasServerConflict(invoice.getUpdatedAt(), change.getCreatedAt(), conflictPolicy)) {
            return null;
        }

        invoice.setId(id);
        invoice.setUser(user);
        invoice.setClient(client);

        // Never renumber an existing invoice. New offline-created invoices get
        // their authoritative, globally unique number from the server.
        LocalDateTime effectiveInvoiceDate = valueOrDefault(payload.getInvoiceDate(), serverTime);
        if (existingInvoice.isEmpty()) {
            InvoiceNumberService.InvoiceNumberResult number = invoiceNumberService
                    .generateNextInvoiceNumber(userId, effectiveInvoiceDate.toLocalDate());
            invoice.setInvoiceNumber(number.invoiceNumber());
            invoice.setFinancialYear(number.financialYear());
            invoice.setSequenceNo(number.sequenceNo());
        }

        invoice.setSubtotal(valueOrDefault(payload.getSubtotal(), 0.0));
        invoice.setDiscount(valueOrDefault(payload.getDiscount(), 0.0));
        invoice.setGrossAmount(valueOrDefault(payload.getGrossAmount(), valueOrDefault(payload.getSubtotal(), 0.0)));
        invoice.setAdvanceApplied(valueOrDefault(payload.getAdvanceApplied(), 0.0));
        invoice.setNetPayable(valueOrDefault(payload.getNetPayable(), valueOrDefault(payload.getTotalAmount(), 0.0)));
        invoice.setTotalAmount(valueOrDefault(payload.getTotalAmount(), 0.0));
        invoice.setPaidAmount(valueOrDefault(payload.getPaidAmount(), 0.0));
        invoice.setPendingAmount(valueOrDefault(payload.getPendingAmount(), invoice.getTotalAmount() - invoice.getPaidAmount()));

        if (payload.getPaymentStatus() != null) {
            invoice.setPaymentStatus(PaymentStatus.valueOf(payload.getPaymentStatus()));
        }
        if (payload.getPaymentMode() != null) {
            invoice.setPaymentMode(PaymentMode.valueOf(payload.getPaymentMode()));
        }

        invoice.setInvoiceDate(effectiveInvoiceDate);
        invoice.setDueDate(payload.getDueDate());
        invoice.setPaymentDate(payload.getPaymentDate());
        invoice.setNotes(payload.getNotes());

        invoice.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (invoice.getCreatedDate() == null) {
            invoice.setCreatedDate(serverTime);
        }

        invoice.setUpdatedAt(serverTime);
        invoice.setVersion(nextVersion(invoice.getVersion()));

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            invoice.markDeleted(serverTime);

            if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                List<ClientWork> linkedWorks = workRepository.findAllById(
                        invoice.getItems().stream()
                                .filter(item -> item.getWork() != null)
                                .map(item -> item.getWork().getId())
                                .toList()
                );

                if (!linkedWorks.isEmpty()) {
                    linkedWorks.forEach(work -> {
                        if (work.getUser() != null && work.getUser().getId().equals(userId)) {
                            work.setBilled(false);
                            work.setInvoice(null);
                            work.setUpdatedAt(serverTime);
                        }
                    });
                    workRepository.saveAll(linkedWorks);
                }
            }

            if (invoice.getItems() != null) {
                invoice.getItems().forEach(item -> item.markDeleted(serverTime));
                invoiceItemRepository.saveAll(invoice.getItems());
            }

        } else {
            invoice.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            invoice.setDeletedAt(payload.getDeletedAt());
        }

        return invoice;
    }

    private InvoiceItem buildInvoiceItem(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime
    ) {
        UUID id = requireEntityId(change, "Invoice item id missing");
        InvoiceItemSyncPayload payload = toPayload(change, InvoiceItemSyncPayload.class);

        UUID invoiceId = requireUuid(payload.getInvoiceId(), "Invoice item invoice id missing");
        UUID workId = requireUuid(payload.getWorkId(), "Invoice item work id missing");
        requirePositive(payload.getRate(), "Invoice item rate must be positive");
        requirePositive(payload.getQuantity(), "Invoice item quantity must be positive");

        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for invoice item"));

        ClientWork work = workRepository.findByIdAndUserId(workId, userId)
                .orElseThrow(() -> new RuntimeException("Work not found for invoice item"));

        if (!"delete".equalsIgnoreCase(change.getOperation())
                && Boolean.TRUE.equals(work.getBilled())
                && work.getInvoice() != null
                && !work.getInvoice().getId().equals(invoiceId)) {
            throw new RuntimeException("Work has already been billed on another invoice");
        }

        InvoiceItem item = invoiceItemRepository.findByIdAndUserId(id, userId)
                .orElseGet(InvoiceItem::new);

        item.setId(id);
        item.setUser(user);
        item.setInvoice(invoice);
        item.setWork(work);
        item.setDescription(payload.getDescription());
        item.setRate(valueOrDefault(payload.getRate(), 0.0));
        item.setQuantity(valueOrDefault(payload.getQuantity(), 1));
        item.setAmount(valueOrDefault(payload.getAmount(), item.getRate() * item.getQuantity()));
        item.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (item.getCreatedAt() == null) {
            item.setCreatedAt(serverTime);
        }

        item.setUpdatedAt(serverTime);

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            item.markDeleted(serverTime);
        } else {
            item.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            item.setDeletedAt(payload.getDeletedAt());

            if (!Boolean.TRUE.equals(item.getIsDeleted())) {
                work.setBilled(true);
                work.setInvoice(invoice);
                work.setUpdatedAt(serverTime);
            }
        }

        return item;
    }

    private ClientLedgerEntry buildLedgerEntry(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime
    ) {
        UUID id = requireEntityId(change, "Ledger entry id missing");
        LedgerEntrySyncPayload payload = toPayload(change, LedgerEntrySyncPayload.class);

        UUID clientId = requireUuid(payload.getClientId(), "Client id missing for ledger entry");
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found for ledger entry"));

        ClientLedgerEntry entry = ledgerEntryRepository.findByIdAndUserId(id, userId)
                .orElseGet(ClientLedgerEntry::new);

        entry.setId(id);
        entry.setUser(user);
        entry.setClient(client);
        entry.setType(LedgerEntryType.valueOf(payload.getType()));
        entry.setDirection(LedgerDirection.valueOf(payload.getDirection()));
        entry.setAmount(valueOrDefault(payload.getAmount(), 0.0));
        entry.setBalanceAfter(valueOrDefault(payload.getBalanceAfter(), 0.0));
        entry.setNotes(payload.getNotes());
        entry.setTransactionDate(valueOrDefault(payload.getTransactionDate(), serverTime));
        entry.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (payload.getInvoiceId() != null) {
            UUID invoiceId = requireUuid(payload.getInvoiceId(), "Invalid invoice id for ledger entry");
            invoiceRepository.findByIdAndUserId(invoiceId, userId).ifPresent(entry::setInvoice);
        } else {
            entry.setInvoice(null);
        }

        if (payload.getPaymentId() != null) {
            UUID paymentId = requireUuid(payload.getPaymentId(), "Invalid payment id for ledger entry");
            paymentRepository.findByPaymentIdAndUserIdAndIsDeletedFalse(paymentId, userId).ifPresent(entry::setPayment);
        } else {
            entry.setPayment(null);
        }

        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(serverTime);
        }

        entry.setUpdatedAt(serverTime);
        entry.setVersion(nextVersion(entry.getVersion()));

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            entry.markDeleted(serverTime);
        } else {
            entry.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            entry.setDeletedAt(payload.getDeletedAt());
        }

        return entry;
    }

    private <T> T toPayload(SyncChangeDto change, Class<T> type) {
        try {
            return objectMapper.convertValue(change.getPayload(), type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid payload discovered for " + change.getEntityType());
        }
    }

    private UUID requireEntityId(SyncChangeDto change, String message) {
        return requireUuid(valueOrDefault(change.getEntityId(), payloadString(change, "id")), message);
    }

    private UUID requireUuid(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(message + ": must be a UUID string");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
    }

    private void requirePositive(Double value, String message) {
        if (value == null || value <= 0) {
            throw new RuntimeException(message);
        }
    }

    private void requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new RuntimeException(message);
        }
    }

    private boolean hasServerConflict(LocalDateTime serverUpdatedAt, LocalDateTime clientChangedAt, String conflictPolicy) {
        if ("CLIENT_WINS".equalsIgnoreCase(conflictPolicy)) {
            return false;
        }
        return serverUpdatedAt != null
                && clientChangedAt != null
                && clientChangedAt.isBefore(serverUpdatedAt);
    }

    private RejectedChangeDto conflict(SyncChangeDto change, String reason) {
        return new RejectedChangeDto(
                change.getChangeId(),
                change.getEntityType(),
                valueOrDefault(change.getEntityId(), payloadString(change, "id")),
                reason == null || reason.isBlank() ? "Sync change rejected by server" : reason
        );
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        return message;
    }

    private String payloadString(SyncChangeDto change, String key) {
        if (change.getPayload() == null) {
            return null;
        }
        Object value = change.getPayload().get(key);
        return value != null ? value.toString() : null;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private Integer nextVersion(Integer version) {
        return version == null ? 1 : version + 1;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private Double valueOrDefault(Double value, Double fallback) {
        return value == null ? fallback : value;
    }

    private Integer valueOrDefault(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private LocalDateTime valueOrDefault(LocalDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value;
    }

    private Map<String, Object> clientToMap(Client c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("phone", c.getPhone());
        m.put("email", c.getEmail());
        m.put("address", c.getAddress());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        m.put("deletedAt", c.getDeletedAt());
        m.put("isDeleted", c.getIsDeleted());
        m.put("deviceId", c.getDeviceId());
        m.put("version", c.getVersion());
        return m;
    }

    private Map<String, Object> workToMap(ClientWork w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("clientId", w.getClient() != null ? w.getClient().getId() : null);
        m.put("clientName", w.getClient() != null ? w.getClient().getName() : null);
        m.put("description", w.getDescription());
        m.put("rate", w.getRate());
        m.put("quantity", w.getQuantity());
        m.put("amount", w.getAmount());
        m.put("workDate", w.getDate());
        m.put("billed", w.getBilled());
        m.put("invoiceId", w.getInvoice() != null ? w.getInvoice().getId() : null);
        m.put("createdAt", w.getCreatedAt());
        m.put("updatedAt", w.getUpdatedAt());
        m.put("deletedAt", w.getDeletedAt());
        m.put("isDeleted", w.getIsDeleted());
        m.put("deviceId", w.getDeviceId());
        m.put("version", w.getVersion());
        return m;
    }

    private Map<String, Object> invoiceToMap(Invoice i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("clientId", i.getClient() != null ? i.getClient().getId() : null);
        m.put("clientName", i.getClient() != null ? i.getClient().getName() : null);
        m.put("invoiceNumber", i.getInvoiceNumber());

        m.put("subtotal", i.getSubtotal());
        m.put("discount", i.getDiscount());
        m.put("grossAmount", i.getGrossAmount());
        m.put("advanceApplied", i.getAdvanceApplied());
        m.put("netPayable", i.getNetPayable());
        m.put("totalAmount", i.getTotalAmount());
        m.put("paidAmount", i.getPaidAmount());
        m.put("pendingAmount", i.getPendingAmount());
        m.put("paymentStatus", i.getPaymentStatus() != null ? i.getPaymentStatus().name() : null);
        m.put("paymentMode", i.getPaymentMode() != null ? i.getPaymentMode().name() : null);

        m.put("invoiceDate", i.getInvoiceDate());
        m.put("dueDate", i.getDueDate());
        m.put("paymentDate", i.getPaymentDate());
        m.put("notes", i.getNotes());

        m.put("createdAt", i.getCreatedDate());
        m.put("createdDate", i.getCreatedDate());
        m.put("updatedAt", i.getUpdatedAt());
        m.put("deletedAt", i.getDeletedAt());
        m.put("isDeleted", i.getIsDeleted());
        m.put("deviceId", i.getDeviceId());
        m.put("version", i.getVersion());
        return m;
    }

    private Map<String, Object> ledgerEntryToMap(ClientLedgerEntry entry) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", entry.getId());
        m.put("clientId", entry.getClient() != null ? entry.getClient().getId() : null);
        m.put("invoiceId", entry.getInvoice() != null ? entry.getInvoice().getId() : null);
        m.put("paymentId", entry.getPayment() != null ? entry.getPayment().getPaymentId() : null);
        m.put("type", entry.getType() != null ? entry.getType().name() : null);
        m.put("direction", entry.getDirection() != null ? entry.getDirection().name() : null);
        m.put("amount", entry.getAmount());
        m.put("balanceAfter", entry.getBalanceAfter());
        m.put("notes", entry.getNotes());
        m.put("transactionDate", entry.getTransactionDate());
        m.put("createdAt", entry.getCreatedAt());
        m.put("updatedAt", entry.getUpdatedAt());
        m.put("deletedAt", entry.getDeletedAt());
        m.put("isDeleted", entry.getIsDeleted());
        m.put("deviceId", entry.getDeviceId());
        m.put("version", entry.getVersion());
        return m;
    }

    private Map<String, Object> invoiceItemToMap(InvoiceItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("invoiceId", item.getInvoice() != null ? item.getInvoice().getId() : null);
        m.put("workId", item.getWork() != null ? item.getWork().getId() : null);
        m.put("description", item.getDescription());
        m.put("rate", item.getRate());
        m.put("quantity", item.getQuantity());
        m.put("amount", item.getAmount());
        m.put("createdAt", item.getCreatedAt());
        m.put("updatedAt", item.getUpdatedAt());
        m.put("deletedAt", item.getDeletedAt());
        m.put("isDeleted", item.getIsDeleted());
        m.put("deviceId", item.getDeviceId());
        return m;
    }

    private static class SyncPageResult {
        private final Map<String, Object> changes;
        private final String nextCursor;
        private final boolean hasMore;

        public SyncPageResult(Map<String, Object> changes, String nextCursor, boolean hasMore) {
            this.changes = changes;
            this.nextCursor = nextCursor;
            this.hasMore = hasMore;
        }

        public Map<String, Object> getChanges() { return changes; }
        public String getNextCursor() { return nextCursor; }
        public boolean isHasMore() { return hasMore; }
    }

    private static class EntityCursor {
        public LocalDateTime time;
        public UUID id;
        public boolean hasMore;

        public EntityCursor(LocalDateTime time, UUID id, boolean hasMore) {
            this.time = time;
            this.id = id;
            this.hasMore = hasMore;
        }
    }

    private static class PullCursor {
        private final java.util.Map<String, EntityCursor> cursors = new java.util.HashMap<>();

        public PullCursor() {}

        static PullCursor from(String cursor) {
            PullCursor pc = new PullCursor();
            if (cursor == null || cursor.isBlank()) return pc;
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor));
                for (String part : decoded.split(";")) {
                    if (part.isBlank()) continue;
                    String[] kv = part.split("=");
                    if (kv.length != 2) continue;
                    String[] vals = kv[1].split("\\|");
                    if (vals.length != 3) continue;
                    LocalDateTime t = vals[0].isBlank() || vals[0].equals("null") ? null : LocalDateTime.parse(vals[0]);
                    UUID i = vals[1].isBlank() || vals[1].equals("null") ? null : UUID.fromString(vals[1]);
                    boolean hasMore = Boolean.parseBoolean(vals[2]);
                    pc.cursors.put(kv[0], new EntityCursor(t, i, hasMore));
                }
            } catch (Exception e) {}
            return pc;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, EntityCursor> e : cursors.entrySet()) {
                EntityCursor ec = e.getValue();
                sb.append(e.getKey()).append("=")
                  .append(ec.time == null ? "null" : ec.time.toString()).append("|")
                  .append(ec.id == null ? "null" : ec.id.toString()).append("|")
                  .append(ec.hasMore).append(";");
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sb.toString().getBytes());
        }

        public EntityCursor get(String type) { return cursors.get(type); }
        public void put(String type, LocalDateTime time, UUID id, boolean hasMore) { cursors.put(type, new EntityCursor(time, id, hasMore)); }
        public boolean hasMoreOverall() {
            return cursors.values().stream().anyMatch(c -> c.hasMore);
        }
    }
}
