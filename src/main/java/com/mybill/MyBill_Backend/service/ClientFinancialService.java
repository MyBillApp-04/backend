package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ClientFinancialSummaryDTO;
import com.mybill.MyBill_Backend.dto.ReceivePaymentRequest;
import com.mybill.MyBill_Backend.dto.ReceivePaymentResponse;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.ClientLedgerEntryRepository;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.mybill.MyBill_Backend.event.PaymentRecordedEvent;
import com.mybill.MyBill_Backend.event.AdvanceBalanceAvailableEvent;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientFinancialService {

    private final ClientLedgerEntryRepository ledgerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public double getAdvanceBalance(UUID clientId, Long userId) {
        Double balance = ledgerRepository.getAdvanceBalance(clientId, userId);
        return balance == null ? 0.0 : Math.max(balance, 0.0);
    }

    public double applyAdvanceToInvoice(Invoice invoice, double requestedAmount, LocalDateTime when) {
        if (invoice == null || invoice.getClient() == null || requestedAmount <= 0) {
            return 0.0;
        }

        Long userId = invoice.getUser().getId();
        double availableAdvance = getAdvanceBalance(invoice.getClient().getId(), userId);
        double applied = Math.min(availableAdvance, requestedAmount);

        if (applied <= 0) {
            return 0.0;
        }

        addLedgerEntry(
                invoice.getClient(),
                invoice,
                null,
                LedgerEntryType.ADVANCE_APPLIED,
                LedgerDirection.CREDIT,
                applied,
                when,
                "Advance applied to invoice " + invoice.getInvoiceNumber(),
                invoice.getDeviceId()
        );

        return applied;
    }

    public ClientLedgerEntry recordInvoiceCreated(Invoice invoice, LocalDateTime when) {
        if (ledgerRepository.existsByInvoiceIdAndTypeAndUserIdAndIsDeletedFalse(
                invoice.getId(),
                LedgerEntryType.INVOICE_CREATED,
                invoice.getUser().getId()
        )) {
            return null;
        }

        double gross = firstNonNull(invoice.getGrossAmount(), invoice.getSubtotal(), invoice.getTotalAmount(), 0.0);
        return addLedgerEntry(
                invoice.getClient(),
                invoice,
                null,
                LedgerEntryType.INVOICE_CREATED,
                LedgerDirection.DEBIT,
                gross,
                when,
                "Invoice created " + invoice.getInvoiceNumber(),
                invoice.getDeviceId()
        );
    }

    @Transactional(readOnly = true)
    public ClientFinancialSummaryDTO getSummary(UUID clientId) {
        Long userId = securityUtils.getCurrentUserId();
        clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));

        List<Object[]> statsList = invoiceRepository.getClientFinancialStats(clientId, userId);
        double totalBilled = 0.0;
        double totalReceived = 0.0;
        double outstanding = 0.0;

        if (statsList != null && !statsList.isEmpty() && statsList.get(0) != null) {
            Object[] stats = statsList.get(0);
            totalBilled = ((Number) stats[0]).doubleValue();
            totalReceived = ((Number) stats[1]).doubleValue();
            outstanding = ((Number) stats[2]).doubleValue();
        }
        double advance = getAdvanceBalance(clientId, userId);

        return ClientFinancialSummaryDTO.builder()
                .clientId(clientId)
                .advanceBalance(roundMoney(advance))
                .outstandingBalance(roundMoney(outstanding))
                .totalBilledAmount(roundMoney(totalBilled))
                .totalReceivedAmount(roundMoney(totalReceived))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ClientLedgerEntry> getLedger(UUID clientId, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));
        return ledgerRepository.findByClientIdAndUserIdAndIsDeletedFalseOrderByTransactionDateDesc(clientId, userId, pageable);
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ReceivePaymentResponse receivePayment(UUID clientId, ReceivePaymentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();
        Client client = clientRepository.findByIdAndUserIdAndIsDeletedFalse(clientId, userId)
                .orElseThrow(() -> new ForbiddenException("Client not found or access denied"));

        double amount = request.getAmount() != null ? request.getAmount().doubleValue() : 0.0;
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        LocalDateTime when = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now();
        Payment payment = Payment.builder()
                .client(client)
                .user(user)
                .amount(amount)
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : PaymentMode.OTHER)
                .date(when)
                .notes(request.getNotes())
                .refundedAmount(0.0)
                .appliedToInvoice(false)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        double remainingReceipt = amount;
        double appliedToInvoices = 0.0;

        List<Invoice> pendingInvoices = invoiceRepository.findPendingInvoicesByClient(clientId, userId);

        for (Invoice invoice : pendingInvoices) {
            if (remainingReceipt <= 0) break;

            double pending = firstNonNull(invoice.getPendingAmount(), invoice.getRemainingAmount(), 0.0);
            double applied = Math.min(remainingReceipt, pending);
            double currentPaid = firstNonNull(invoice.getPaidAmount(), 0.0);
            double newPaid = currentPaid + applied;
            double newPending = Math.max(firstNonNull(invoice.getTotalAmount(), 0.0) - newPaid, 0.0);

            invoice.setPaidAmount(roundMoney(newPaid));
            invoice.setPendingAmount(roundMoney(newPending));
            invoice.setRemainingAmount(roundMoney(newPending));
            invoice.setPaymentMode(savedPayment.getPaymentMode());
            invoice.setPaymentDate(when);
            invoice.setPaymentStatus(statusFor(firstNonNull(invoice.getTotalAmount(), 0.0), newPaid));
            invoice.setUpdatedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            addLedgerEntry(
                    client,
                    invoice,
                    savedPayment,
                    LedgerEntryType.PAYMENT_RECEIVED,
                    LedgerDirection.CREDIT,
                    applied,
                    when,
                    "Payment applied to invoice " + invoice.getInvoiceNumber(),
                    request.getDeviceId()
            );

            savedPayment.setInvoice(invoice);
            savedPayment.setAppliedToInvoice(true);
            remainingReceipt = roundMoney(remainingReceipt - applied);
            appliedToInvoices = roundMoney(appliedToInvoices + applied);

            eventPublisher.publishEvent(new PaymentRecordedEvent(this, savedPayment, invoice, applied, newPending));
        }

        double addedToAdvance = 0.0;
        if (remainingReceipt > 0) {
            addedToAdvance = remainingReceipt;
            addLedgerEntry(
                    client,
                    null,
                    savedPayment,
                    LedgerEntryType.ADVANCE_RECEIVED,
                    LedgerDirection.CREDIT,
                    remainingReceipt,
                    when,
                    request.getNotes() != null ? request.getNotes() : "Money received",
                    request.getDeviceId()
            );

            eventPublisher.publishEvent(new AdvanceBalanceAvailableEvent(this, client, user, remainingReceipt));
        }

        paymentRepository.save(savedPayment);

        return ReceivePaymentResponse.builder()
                .receivedAmount(roundMoney(amount))
                .appliedToInvoices(roundMoney(appliedToInvoices))
                .addedToAdvance(roundMoney(addedToAdvance))
                .summary(getSummary(clientId))
                .build();
    }

    public ClientLedgerEntry addLedgerEntry(
            Client client,
            Invoice invoice,
            Payment payment,
            LedgerEntryType type,
            LedgerDirection direction,
            double amount,
            LocalDateTime when,
            String notes,
            String deviceId
    ) {
        User user = invoice != null && invoice.getUser() != null ? invoice.getUser() : securityUtils.getCurrentUser();
        double balanceAfter = computeLedgerBalance(client.getId(), user.getId(), type, direction, amount);

        ClientLedgerEntry entry = ClientLedgerEntry.builder()
                .client(client)
                .invoice(invoice)
                .payment(payment)
                .user(user)
                .type(type)
                .direction(direction)
                .amount(roundMoney(amount))
                .balanceAfter(roundMoney(balanceAfter))
                .transactionDate(when != null ? when : LocalDateTime.now())
                .notes(notes)
                .deviceId(deviceId)
                .build();

        return ledgerRepository.save(entry);
    }

    private double computeLedgerBalance(UUID clientId, Long userId, LedgerEntryType type, LedgerDirection direction, double amount) {
        double current = getAdvanceBalance(clientId, userId);
        double delta = switch (type) {
            case ADVANCE_RECEIVED -> amount;
            case ADVANCE_APPLIED -> -amount;
            case ADJUSTMENT -> direction == LedgerDirection.CREDIT ? amount : -amount;
            default -> 0.0;
        };
        return Math.max(current + delta, 0.0);
    }

    private PaymentStatus statusFor(double total, double paid) {
        if (total <= 0 || paid >= total) return PaymentStatus.PAID;
        if (paid > 0) return PaymentStatus.PARTIALLY_PAID;
        return PaymentStatus.UNPAID;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double firstNonNull(Double... values) {
        for (Double value : values) {
            if (value != null) return value;
        }
        return 0.0;
    }

    private LocalDateTime firstNonNullDate(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) return value;
        }
        return LocalDateTime.MAX;
    }
}
