package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.sync.*;
import com.mybill.MyBill_Backend.dto.sync.payload.*;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.*;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final QuotationService quotationService;
    private final BusinessProfileRepository businessProfileRepository;

    /** Canonical public base URL — used when building quotation share links in sync pull responses. */
    @Value("${app.public-url.base-url:https://mybill-backend-vckc.onrender.com}")
    private String publicBaseUrl;

    @jakarta.annotation.PostConstruct
    public void initMixins() {
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module());
        objectMapper.addMixIn(Client.class, EntityMixin.class);
        objectMapper.addMixIn(ClientWork.class, EntityMixin.class);
        objectMapper.addMixIn(Invoice.class, EntityMixin.class);
        objectMapper.addMixIn(InvoiceItem.class, EntityMixin.class);
        objectMapper.addMixIn(ClientLedgerEntry.class, EntityMixin.class);
        objectMapper.addMixIn(Quotation.class, EntityMixin.class);
        objectMapper.addMixIn(QuotationItem.class, EntityMixin.class);
        objectMapper.addMixIn(CatalogItem.class, EntityMixin.class);
        objectMapper.addMixIn(Expense.class, EntityMixin.class);
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(
        ignoreUnknown = true,
        value = {"hibernateLazyInitializer", "handler", "client", "invoice", "items", "user", "payment", "quotation"}
    )
    private interface EntityMixin {}

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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

        if (request.getChanges() != null && !request.getChanges().isEmpty()) {
            // Running advance-balance per client, recomputed server-side across the whole
            // batch so ledger entries applied together do not drift from the authoritative sum.
            Map<UUID, Double> runningBalances = new HashMap<>();
            // Maps invoiceId -> {provisional client number, authoritative server number} so
            // work.previousInvoiceNumber and ledger notes can be backfilled after numbering.
            Map<UUID, InvoiceNumberBackfill> invoiceNumberBackfills = new HashMap<>();
            // Authoritative subtotal per invoice, derived server-side from the rate x quantity
            // of the invoice_item changes in this batch so a modified client cannot understate it.
            Map<UUID, Double> authoritativeSubtotals = computeAuthoritativeSubtotals(request.getChanges());
            try {
                TransactionTemplate batchTemplate = new TransactionTemplate(transactionManager);
                final String finalConflictPolicy = conflictPolicy;
                final int[] finalConflictCount = new int[1];
                batchTemplate.executeWithoutResult(status -> {
                    User managedUser = userRepository.getReferenceById(userId);
                    for (SyncChangeDto change : orderedChanges(request.getChanges())) {
                        boolean applied = applyChangeInternal(change, managedUser, userId, request.getDeviceId(), serverTime, finalConflictPolicy, runningBalances, invoiceNumberBackfills, authoritativeSubtotals);
                        if (applied) {
                            accepted.add(change.getChangeId());
                        } else {
                            finalConflictCount[0]++;
                            rejected.add(conflict(change, "Server version is newer; change rejected by conflict policy"));
                        }
                    }
                });
                conflictCount = finalConflictCount[0];
            } catch (Exception batchEx) {
                // Rollback and fallback to a single REQUIRES_NEW transaction.
                // The entire change set is applied all-or-nothing: a failure in any part
                // (e.g. an invoice and its items/ledger/works) rejects the whole group rather
                // than leaving a partially applied multi-entity invoice behind.
                accepted.clear();
                rejected.clear();
                conflictCount = 0;
                try {
                    TransactionTemplate retryTemplate = new TransactionTemplate(transactionManager);
                    retryTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
                    final String finalConflictPolicy = conflictPolicy;
                    final int[] finalConflictCount = new int[1];
                    retryTemplate.executeWithoutResult(status -> {
                        User managedUser = userRepository.getReferenceById(userId);
                        for (SyncChangeDto change : orderedChanges(request.getChanges())) {
                            boolean applied = applyChangeInternal(change, managedUser, userId, request.getDeviceId(), serverTime, finalConflictPolicy, runningBalances, invoiceNumberBackfills, authoritativeSubtotals);
                            if (applied) {
                                accepted.add(change.getChangeId());
                            } else {
                                finalConflictCount[0]++;
                                rejected.add(conflict(change, "Server version is newer; change rejected by conflict policy"));
                            }
                        }
                    });
                    conflictCount = finalConflictCount[0];
                } catch (StackOverflowError e) {
                    throw new IllegalStateException("Server sync recursion while saving change group", e);
                } catch (Exception groupEx) {
                    // The whole group failed atomically: reject every change so the client
                    // keeps them pending for retry instead of a partial commit.
                    accepted.clear();
                    rejected.clear();
                    conflictCount = 0;
                    for (SyncChangeDto change : orderedChanges(request.getChanges())) {
                        rejected.add(conflict(change, rootCauseMessage(groupEx)));
                    }
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

    private boolean applyChangeInternal(
            SyncChangeDto change,
            User managedUser,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy,
            Map<UUID, Double> runningBalances,
            Map<UUID, InvoiceNumberBackfill> invoiceNumberBackfills,
            Map<UUID, Double> authoritativeSubtotals
    ) {
        boolean applied;
        switch (change.getEntityType()) {
            case "client" -> {
                applied = applyClientChange(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
            }
            case "work" -> {
                ClientWork w = buildWork(change, managedUser, userId, deviceId, serverTime, conflictPolicy, invoiceNumberBackfills);
                applied = w != null;
                if (applied) workRepository.saveAndFlush(w);
            }
            case "invoice" -> {
                boolean isNew = invoiceRepository.findByIdAndUserId(
                        UUID.fromString(change.getEntityId()), userId).isEmpty();
                Invoice i = buildInvoice(change, managedUser, userId, deviceId, serverTime, conflictPolicy, invoiceNumberBackfills, authoritativeSubtotals);
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
                ClientLedgerEntry le = buildLedgerEntry(change, managedUser, userId, deviceId, serverTime, runningBalances, invoiceNumberBackfills);
                applied = le != null;
                if (applied) ledgerEntryRepository.saveAndFlush(le);
            }
            case "quotation" -> {
                Quotation q = buildQuotation(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
                applied = q != null;
                if (applied) quotationRepository.saveAndFlush(q);
            }
            case "quotation_item" -> {
                QuotationItem qi = buildQuotationItem(change, managedUser, userId, deviceId, serverTime);
                applied = qi != null;
                if (applied) quotationItemRepository.saveAndFlush(qi);
            }
            case "catalog_item" -> {
                CatalogItem ci = buildCatalogItem(change, managedUser, userId, deviceId, serverTime, conflictPolicy);
                applied = ci != null;
                if (applied) catalogItemRepository.saveAndFlush(ci);
            }
            default -> throw new RuntimeException("Unsupported entity type: " + change.getEntityType());
        }
        return applied;
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
        client.setState(payload.getState());
        client.setGstin(payload.getGstin());
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
        client.setState(payload.getState());
        client.setGstin(payload.getGstin());
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
                    case "invoice" -> 1;
                    case "work" -> 2;
                    case "invoice_item" -> 3;
                    case "ledger_entry" -> 4;
                    case "quotation" -> 5;
                    case "quotation_item" -> 6;
                    case "catalog_item" -> 7;
                    default -> 8;
                }))
                .toList();
    }

    /**
     * Computes the authoritative subtotal per invoice from the invoice_item changes in
     * this batch, using rate x quantity for each non-deleted item. This prevents a
     * modified client from understating the invoice total.
     */
    private Map<UUID, Double> computeAuthoritativeSubtotals(List<SyncChangeDto> changes) {
        Map<UUID, Double> subtotals = new HashMap<>();
        for (SyncChangeDto change : changes) {
            if (!"invoice_item".equals(change.getEntityType())) {
                continue;
            }
            if ("delete".equalsIgnoreCase(change.getOperation())) {
                continue;
            }
            if (change.getPayload() == null) {
                continue;
            }
            Object invoiceIdObj = change.getPayload().get("invoiceId");
            if (invoiceIdObj == null || invoiceIdObj.toString().isBlank()) {
                continue;
            }
            UUID invoiceId;
            try {
                invoiceId = UUID.fromString(invoiceIdObj.toString());
            } catch (IllegalArgumentException e) {
                continue;
            }
            double rate = valueOrDefault(toDouble(change.getPayload().get("rate")), 0.0);
            int quantity = valueOrDefault(toInt(change.getPayload().get("quantity")), 1);
            subtotals.merge(invoiceId, roundMoney(rate * quantity), Double::sum);
        }
        return subtotals;
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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

        // Quotations
        EntityCursor qc = cursor.get("quotation");
        Page<Quotation> quotationPage;
        if (qc != null && qc.time != null && qc.id != null) {
            quotationPage = quotationRepository.findByUserIdWithKeyset(userId, qc.time, qc.id, pageable);
        } else {
            quotationPage = since == null ? quotationRepository.findByUserId(userId, pageable) : quotationRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!quotationPage.isEmpty()) {
            Quotation last = quotationPage.getContent().get(quotationPage.getContent().size() - 1);
            cursor.put("quotation", last.getUpdatedAt(), last.getId(), quotationPage.hasNext());
        } else {
            cursor.put("quotation", null, null, false);
        }

        // Quotation Items
        EntityCursor qic = cursor.get("quotation_item");
        Page<QuotationItem> quotationItemPage;
        if (qic != null && qic.time != null && qic.id != null) {
            quotationItemPage = quotationItemRepository.findByUserIdWithKeyset(userId, qic.time, qic.id, pageable);
        } else {
            quotationItemPage = since == null ? quotationItemRepository.findByUserId(userId, pageable) : quotationItemRepository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        }
        if (!quotationItemPage.isEmpty()) {
            QuotationItem last = quotationItemPage.getContent().get(quotationItemPage.getContent().size() - 1);
            cursor.put("quotation_item", last.getUpdatedAt(), last.getId(), quotationItemPage.hasNext());
        } else {
            cursor.put("quotation_item", null, null, false);
        }

        changes.put("clients", clientPage.getContent().stream().map(this::clientToMap).toList());
        changes.put("works", workPage.getContent().stream().map(this::workToMap).toList());
        changes.put("invoices", invoicePage.getContent().stream().map(this::invoiceToMap).toList());
        changes.put("invoiceItems", itemPage.getContent().stream().map(this::invoiceItemToMap).toList());
        changes.put("ledgerEntries", ledgerPage.getContent().stream().map(this::ledgerEntryToMap).toList());
        changes.put("quotations", quotationPage.getContent().stream().map(this::quotationToMap).toList());
        changes.put("quotationItems", quotationItemPage.getContent().stream().map(this::quotationItemToMap).toList());

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
        client.setState(payload.getState());
        client.setGstin(payload.getGstin());
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
            String conflictPolicy,
            Map<UUID, InvoiceNumberBackfill> invoiceNumberBackfills
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
                .orElse(null);
        if (client == null) {
            return null;
        }

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
        // Amount is recomputed server-side from rate x quantity, not trusted from the client.
        work.setAmount(roundMoney(work.getRate() * work.getQuantity()));
        work.setDate(valueOrDefault(payload.getWorkDate(), serverTime));
        work.setBilled(Boolean.TRUE.equals(payload.getBilled()));
        // Backfill the authoritative server-issued invoice number if this work was billed
        // on an invoice created in this same batch (the client only knows the provisional number).
        work.setPreviousInvoiceNumber(resolveInvoiceNumber(payload.getPreviousInvoiceNumber(),
                payload.getInvoiceId(), invoiceNumberBackfills));
        work.setLastBilledDate(payload.getLastBilledDate());
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
            String conflictPolicy,
            Map<UUID, InvoiceNumberBackfill> invoiceNumberBackfills,
            Map<UUID, Double> authoritativeSubtotals
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

        if (payload.getQuotationId() != null && !payload.getQuotationId().isBlank()) {
            UUID qId = requireUuid(payload.getQuotationId(), "Invalid quotation id for invoice");
            quotationRepository.findByIdAndUserId(qId, userId).ifPresent(invoice::setQuotation);
        } else {
            invoice.setQuotation(null);
        }

        // Never renumber an existing invoice. New offline-created invoices get
        // their authoritative, globally unique number from the server.
        LocalDateTime effectiveInvoiceDate = valueOrDefault(payload.getInvoiceDate(), serverTime);
        if (existingInvoice.isEmpty()) {
            InvoiceNumberService.InvoiceNumberResult number = invoiceNumberService
                    .generateNextInvoiceNumber(userId, effectiveInvoiceDate.toLocalDate());
            invoice.setInvoiceNumber(number.invoiceNumber());
            invoice.setFinancialYear(number.financialYear());
            invoice.setSequenceNo(number.sequenceNo());
            // Record provisional -> authoritative mapping so dependent work/ledger changes
            // created offline in this batch are backfilled with the real number.
            invoiceNumberBackfills.put(id, new InvoiceNumberBackfill(payload.getInvoiceNumber(), number.invoiceNumber()));
        }

        // ---- Server-side authoritative recomputation ----
        // Financial amounts are NOT trusted from the client. We take only the
        // discount as input and recompute subtotal (from the batch's invoice items),
        // the GST snapshot and totals via TaxCalculator (mirrors generateInvoiceForUser).
        double subtotal = roundMoney(valueOrDefault(authoritativeSubtotals.get(id),
                valueOrDefault(payload.getSubtotal(), 0.0)));
        double discount = roundMoney(valueOrDefault(payload.getDiscount(), 0.0));
        double grossAmount = roundMoney(subtotal - discount);
        if (grossAmount < 0) {
            throw new RuntimeException("Discount cannot be greater than subtotal");
        }

        double safeRate = roundMoney(valueOrDefault(payload.getTaxRate(), 0.0));
        TaxType resolvedType = payload.getTaxType() != null && !payload.getTaxType().isBlank()
                ? TaxType.valueOf(payload.getTaxType())
                : null;
        if (resolvedType == null) {
            String businessState = businessProfileRepository.findByUserId(userId)
                    .map(com.mybill.MyBill_Backend.entity.BusinessProfile::getState)
                    .orElse(null);
            resolvedType = safeRate > 0.0
                    ? TaxCalculator.resolveTaxType(businessState, invoice.getClientState() != null
                            ? invoice.getClientState()
                            : client.getState())
                    : TaxType.NONE;
        }
        TaxCalculator.TaxBreakdown tax = TaxCalculator.calculate(grossAmount, safeRate, resolvedType);

        double total = roundMoney(tax.total);
        double advanceApplied = roundMoney(Math.max(0.0, Math.min(valueOrDefault(payload.getAdvanceApplied(), 0.0), total)));
        double netPayable = TaxCalculator.amountDue(total, advanceApplied);
        double cachedPaidAmount = roundMoney(Math.max(0.0, Math.min(valueOrDefault(payload.getPaidAmount(), 0.0), netPayable)));
        double pendingAmount = roundMoney(Math.max(netPayable - cachedPaidAmount, 0.0));

        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setGrossAmount(grossAmount);
        invoice.setTaxRate(tax.rate);
        invoice.setTaxType(tax.type);
        invoice.setTaxableAmount(tax.taxableAmount);
        invoice.setTaxAmount(tax.taxAmount);
        invoice.setCgstAmount(tax.cgstAmount);
        invoice.setSgstAmount(tax.sgstAmount);
        invoice.setIgstAmount(tax.igstAmount);
        invoice.setTotal(total);
        invoice.setAdvanceApplied(advanceApplied);
        invoice.setNetPayable(netPayable);
        invoice.setTotalAmount(netPayable);
        invoice.setPaidAmount(cachedPaidAmount);
        invoice.setPendingAmount(pendingAmount);
        invoice.setRemainingAmount(pendingAmount);

        if (netPayable <= 0 || cachedPaidAmount >= netPayable) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
        } else if (cachedPaidAmount > 0) {
            invoice.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            invoice.setPaymentStatus(PaymentStatus.UNPAID);
        }
        if (payload.getPaymentMode() != null) {
            invoice.setPaymentMode(PaymentMode.valueOf(payload.getPaymentMode()));
        }

        invoice.setInvoiceDate(effectiveInvoiceDate);
        invoice.setDueDate(payload.getDueDate());
        invoice.setPaymentDate(payload.getPaymentDate());
        invoice.setNotes(payload.getNotes());

        invoice.setClientState(payload.getClientState());
        invoice.setClientGstin(payload.getClientGstin());

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
        UUID workId = parseWorkId(payload.getWorkId());
        requirePositive(payload.getRate(), "Invoice item rate must be positive");
        requirePositive(payload.getQuantity(), "Invoice item quantity must be positive");

        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElse(null);
        if (invoice == null) {
            return null;
        }

        ClientWork work = workId != null ? workRepository.findByIdAndUserId(workId, userId).orElse(null) : null;

        if (work != null && !"delete".equalsIgnoreCase(change.getOperation())
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
        item.setDimension(payload.getDimension());
        item.setKgs(payload.getKgs());
        item.setRate(valueOrDefault(payload.getRate(), 0.0));
        item.setQuantity(valueOrDefault(payload.getQuantity(), 1));
        // Amount is recomputed server-side from rate x quantity; the client-supplied
        // amount is not trusted so totals cannot be understated by a modified client.
        item.setAmount(roundMoney(item.getRate() * item.getQuantity()));
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

            if (!Boolean.TRUE.equals(item.getIsDeleted()) && work != null) {
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
            LocalDateTime serverTime,
            Map<UUID, Double> runningBalances,
            Map<UUID, InvoiceNumberBackfill> invoiceNumberBackfills
    ) {
        UUID id = requireEntityId(change, "Ledger entry id missing");
        LedgerEntrySyncPayload payload = toPayload(change, LedgerEntrySyncPayload.class);

        UUID clientId = requireUuid(payload.getClientId(), "Client id missing for ledger entry");
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElse(null);
        if (client == null) {
            return null;
        }

        ClientLedgerEntry entry = ledgerEntryRepository.findByIdAndUserId(id, userId)
                .orElseGet(ClientLedgerEntry::new);

        entry.setId(id);
        entry.setUser(user);
        entry.setClient(client);
        entry.setType(LedgerEntryType.valueOf(payload.getType()));
        entry.setDirection(LedgerDirection.valueOf(payload.getDirection()));
        entry.setAmount(valueOrDefault(payload.getAmount(), 0.0));
        entry.setNotes(resolveInvoiceNumberInNotes(payload.getNotes(), payload.getInvoiceId(), invoiceNumberBackfills));
        entry.setTransactionDate(valueOrDefault(payload.getTransactionDate(), serverTime));
        entry.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        // balanceAfter is NOT trusted from the client. It is recomputed server-side so the
        // stored running balance always matches the authoritative sum across the whole batch.
        if (!"delete".equalsIgnoreCase(change.getOperation())) {
            double balance = runningBalances.computeIfAbsent(clientId,
                    k -> valueOrDefault(ledgerEntryRepository.getAdvanceBalance(k, userId), 0.0));
            balance = Math.max(roundMoney(balance + ledgerDelta(entry.getType(), entry.getDirection(), entry.getAmount())), 0.0);
            runningBalances.put(clientId, balance);
            entry.setBalanceAfter(balance);
        } else {
            entry.setBalanceAfter(valueOrDefault(entry.getBalanceAfter(), 0.0));
        }

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

    private double ledgerDelta(LedgerEntryType type, LedgerDirection direction, Double amount) {
        double a = amount == null ? 0.0 : amount;
        return switch (type) {
            case ADVANCE_RECEIVED -> a;
            case ADVANCE_APPLIED -> -a;
            case ADJUSTMENT -> direction == LedgerDirection.CREDIT ? a : -a;
            default -> 0.0;
        };
    }

    private record InvoiceNumberBackfill(String provisional, String authoritative) {}

    private String resolveInvoiceNumber(String current, String invoiceId, Map<UUID, InvoiceNumberBackfill> backfills) {
        if (invoiceId == null || invoiceId.isBlank() || backfills == null || backfills.isEmpty()) {
            return current;
        }
        InvoiceNumberBackfill backfill;
        try {
            backfill = backfills.get(UUID.fromString(invoiceId));
        } catch (IllegalArgumentException e) {
            return current;
        }
        if (backfill == null || backfill.authoritative == null) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return backfill.authoritative;
        }
        return backfill.provisional != null && backfill.provisional.equals(current)
                ? backfill.authoritative
                : current;
    }

    private String resolveInvoiceNumberInNotes(String notes, String invoiceId, Map<UUID, InvoiceNumberBackfill> backfills) {
        if (notes == null || notes.isBlank() || invoiceId == null || invoiceId.isBlank() || backfills == null || backfills.isEmpty()) {
            return notes;
        }
        InvoiceNumberBackfill backfill;
        try {
            backfill = backfills.get(UUID.fromString(invoiceId));
        } catch (IllegalArgumentException e) {
            return notes;
        }
        if (backfill == null || backfill.provisional == null || backfill.provisional.isBlank() || backfill.authoritative == null) {
            return notes;
        }
        return notes.replace(backfill.provisional, backfill.authoritative);
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

    private UUID parseWorkId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
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

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> entityToMap(Object entity) {
        Map<String, Object> m = objectMapper.convertValue(entity, Map.class);
        if (entity instanceof ClientWork w) {
            m.put("clientId", w.getClient() != null ? w.getClient().getId() : null);
            m.put("clientName", w.getClient() != null ? w.getClient().getName() : null);
            m.put("invoiceId", w.getInvoice() != null ? w.getInvoice().getId() : null);
            m.put("workDate", w.getDate());
        } else if (entity instanceof Invoice i) {
            m.put("clientId", i.getClient() != null ? i.getClient().getId() : null);
            m.put("clientName", i.getClient() != null ? i.getClient().getName() : null);
            m.put("clientState", i.getClientState());
            m.put("clientGstin", i.getClientGstin());
            m.put("quotationId", i.getQuotation() != null ? i.getQuotation().getId() : null);
            m.put("createdAt", i.getCreatedDate());
            m.put("createdDate", i.getCreatedDate());
        } else if (entity instanceof InvoiceItem ii) {
            m.put("invoiceId", ii.getInvoice() != null ? ii.getInvoice().getId() : null);
            m.put("workId", ii.getWork() != null ? ii.getWork().getId() : null);
            m.put("dimension", ii.getDimension());
            m.put("kgs", ii.getKgs());
        } else if (entity instanceof ClientLedgerEntry le) {
            m.put("clientId", le.getClient() != null ? le.getClient().getId() : null);
            m.put("invoiceId", le.getInvoice() != null ? le.getInvoice().getId() : null);
            m.put("paymentId", le.getPayment() != null ? le.getPayment().getPaymentId() : null);
        } else if (entity instanceof Quotation q) {
            m.put("clientId", q.getClient() != null ? q.getClient().getId() : null);
            m.put("clientName", q.getClient() != null ? q.getClient().getName() : null);
            String rawToken = q.getPublicToken();
            m.put("publicToken", rawToken);
            if (rawToken != null) {
                String base = publicBaseUrl != null && !publicBaseUrl.isBlank()
                        ? publicBaseUrl.trim().replaceAll("/+$", "")
                        : "https://mybill-backend-vckc.onrender.com";
                m.put("publicResponseUrl", base + "/q/" + rawToken);
            } else {
                m.put("publicResponseUrl", null);
            }
        } else if (entity instanceof QuotationItem qi) {
            m.put("quotationId", qi.getQuotation() != null ? qi.getQuotation().getId() : null);
        } else if (entity instanceof CatalogItem ci) {
            m.put("userId", ci.getUser() != null ? ci.getUser().getId() : null);
        }
        return m;
    }

    private Map<String, Object> clientToMap(Client c) {
        return entityToMap(c);
    }

    private Map<String, Object> workToMap(ClientWork w) {
        return entityToMap(w);
    }

    private Map<String, Object> invoiceToMap(Invoice i) {
        return entityToMap(i);
    }

    private Map<String, Object> ledgerEntryToMap(ClientLedgerEntry entry) {
        return entityToMap(entry);
    }

    private Map<String, Object> invoiceItemToMap(InvoiceItem item) {
        return entityToMap(item);
    }

    private Quotation buildQuotation(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Quotation id missing");
        QuotationSyncPayload payload = toPayload(change, QuotationSyncPayload.class);

        UUID clientId = requireUuid(payload.getClientId(), "Client id missing for quotation");

        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new RuntimeException("Client not found for quotation"));

        Optional<Quotation> existingQuotation = quotationRepository.findByIdAndUserId(id, userId);
        Quotation quotation = existingQuotation.orElseGet(Quotation::new);

        if (hasServerConflict(quotation.getUpdatedAt(), change.getCreatedAt(), conflictPolicy)) {
            return null;
        }

        quotation.setId(id);
        quotation.setUser(user);
        quotation.setClient(client);

        LocalDateTime effectiveQuotationDate = valueOrDefault(payload.getIssueDate(), serverTime);
        if (existingQuotation.isEmpty()) {
            String nextNumber = quotationService.generateNextQuotationNumber(userId, effectiveQuotationDate);
            quotation.setQuotationNumber(nextNumber);
        }

        QuotationStatus newStatus = QuotationStatus.valueOf(valueOrDefault(payload.getStatus(), "DRAFT").toUpperCase());
        if (existingQuotation.isPresent()) {
            QuotationStatus currentStatus = existingQuotation.get().getStatus();
            if (currentStatus == QuotationStatus.ACCEPTED ||
                currentStatus == QuotationStatus.REJECTED ||
                currentStatus == QuotationStatus.DISCUSSION_REQUESTED) {
                if (newStatus == QuotationStatus.DRAFT || newStatus == QuotationStatus.SENT) {
                    newStatus = currentStatus;
                }
            }
        }
        quotation.setStatus(newStatus);
        quotation.setIssueDate(effectiveQuotationDate);
        quotation.setValidUntilDate(payload.getValidUntilDate());
        quotation.setNotes(payload.getNotes());
        quotation.setTermsAndConditions(payload.getTermsAndConditions());
        quotation.setPdfUrl(payload.getPdfUrl());
        quotation.setPdfPath(payload.getPdfPath());

        quotation.setSubtotal(valueOrDefault(payload.getSubtotal(), 0.0));
        quotation.setDiscount(valueOrDefault(payload.getDiscount(), 0.0));
        quotation.setGrossAmount(valueOrDefault(payload.getGrossAmount(), valueOrDefault(payload.getSubtotal(), 0.0)));
        quotation.setTotalAmount(valueOrDefault(payload.getTotalAmount(), 0.0));
        quotation.setNetPayable(valueOrDefault(payload.getNetPayable(), valueOrDefault(payload.getTotalAmount(), 0.0)));

        quotation.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        String rawToken = payload.getPublicToken();
        if (rawToken != null && rawToken.matches("^[A-Za-z0-9_-]{43}$")) {
            String tokenHash = QuotationPublicResponseService.hashToken(rawToken);
            quotation.setPublicTokenHash(tokenHash);
            quotation.setPublicToken(rawToken); // persist raw token so sync pull can echo it back
            if (quotation.getTokenCreatedAt() == null) {
                quotation.setTokenCreatedAt(serverTime);
            }
            if (quotation.getTokenExpiresAt() == null) {
                quotation.setTokenExpiresAt(payload.getValidUntilDate() != null ? payload.getValidUntilDate() : serverTime.plusDays(30));
            }
        } else if (quotation.getPublicTokenHash() == null) {
            String generatedToken = QuotationPublicResponseService.generateRandomToken();
            quotation.setPublicTokenHash(QuotationPublicResponseService.hashToken(generatedToken));
            quotation.setPublicToken(generatedToken); // persist raw token for sync pull echo-back
            quotation.setTokenCreatedAt(serverTime);
            quotation.setTokenExpiresAt(payload.getValidUntilDate() != null ? payload.getValidUntilDate() : serverTime.plusDays(30));
        }

        if (quotation.getCreatedAt() == null) {
            quotation.setCreatedAt(serverTime);
        }

        quotation.setUpdatedAt(serverTime);
        quotation.setVersion(nextVersion(quotation.getVersion()));

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            quotation.markDeleted(serverTime);

            if (quotation.getItems() != null) {
                quotation.getItems().forEach(item -> item.markDeleted(serverTime));
                quotationItemRepository.saveAll(quotation.getItems());
            }
        } else {
            quotation.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            quotation.setDeletedAt(payload.getDeletedAt());
        }

        return quotation;
    }

    private QuotationItem buildQuotationItem(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime
    ) {
        UUID id = requireEntityId(change, "Quotation item id missing");
        QuotationItemSyncPayload payload = toPayload(change, QuotationItemSyncPayload.class);

        UUID quotationId = requireUuid(payload.getQuotationId(), "Quotation item parent id missing");
        requirePositive(payload.getAmount(), "Quotation item amount must be non-negative");
        requirePositive(payload.getQuantity(), "Quotation item quantity must be positive");

        Quotation quotation = quotationRepository.findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new RuntimeException("Quotation not found for quotation item"));

        QuotationItem item = quotationItemRepository.findByIdAndUserId(id, userId)
                .orElseGet(QuotationItem::new);

        item.setId(id);
        item.setUser(user);
        item.setQuotation(quotation);
        item.setDescription(payload.getDescription());
        item.setDimension(payload.getDimension());
        item.setQuantity(valueOrDefault(payload.getQuantity(), 1));
        item.setKgs(payload.getKgs());
        item.setAmount(valueOrDefault(payload.getAmount(), 0.0));
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
        }

        return item;
    }

    private CatalogItem buildCatalogItem(
            SyncChangeDto change,
            User user,
            Long userId,
            String deviceId,
            LocalDateTime serverTime,
            String conflictPolicy
    ) {
        UUID id = requireEntityId(change, "Catalog item id missing");
        CatalogItemSyncPayload payload = toPayload(change, CatalogItemSyncPayload.class);

        requirePositive(payload.getDefaultRate(), "Catalog item default rate must be non-negative");
        requirePositive(payload.getDefaultTaxRate(), "Catalog item default tax rate must be non-negative");

        CatalogItem item = catalogItemRepository.findByIdAndUserId(id, userId)
                .orElseGet(CatalogItem::new);

        item.setId(id);
        item.setUser(user);
        item.setName(payload.getName());
        item.setDescription(payload.getDescription());
        item.setType(payload.getType());
        item.setDefaultRate(payload.getDefaultRate());
        item.setDefaultTaxRate(payload.getDefaultTaxRate());
        item.setUnit(payload.getUnit());
        item.setDimension(payload.getDimension());
        item.setKgs(payload.getKgs());
        item.setIsActive(payload.getIsActive() != null ? payload.getIsActive() : true);
        item.setDeviceId(valueOrDefault(payload.getDeviceId(), deviceId));

        if (item.getCreatedAt() == null) {
            item.setCreatedAt(serverTime);
        }

        item.setUpdatedAt(serverTime);

        if ("delete".equalsIgnoreCase(change.getOperation())) {
            item.setIsDeleted(true);
            item.setDeletedAt(serverTime);
        } else {
            item.setIsDeleted(Boolean.TRUE.equals(payload.getIsDeleted()));
            item.setDeletedAt(payload.getDeletedAt());
        }

        return item;
    }

    private Map<String, Object> quotationToMap(Quotation q) {
        return entityToMap(q);
    }

    private Map<String, Object> quotationItemToMap(QuotationItem item) {
        return entityToMap(item);
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
