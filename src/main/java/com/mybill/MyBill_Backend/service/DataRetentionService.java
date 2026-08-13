package com.mybill.MyBill_Backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final EntityManager entityManager;

    @Value("${app.retention.soft-delete-days:30}")
    private int softDeleteRetentionDays;

    @Value("${app.retention.log-retention-days:180}")
    private int logRetentionDays;

    /**
     * Scheduled task to run daily.
     * By default, runs at 2:00 AM.
     */
    @Scheduled(cron = "${app.retention.cron:0 0 2 * * ?}")
    @Transactional
    public void runRetentionPolicy() {
        log.info("Starting GDPR data retention cleanup process...");

        LocalDateTime softDeleteCutoff = LocalDateTime.now().minusDays(softDeleteRetentionDays);
        LocalDateTime logCutoff = LocalDateTime.now().minusDays(logRetentionDays);

        try {
            // 1. Delete dependents of soft-deleted payments (FraudChecks)
            int deletedFraudChecks = entityManager.createQuery(
                    "DELETE FROM FraudCheck fc WHERE fc.payment.isDeleted = true AND fc.payment.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} FraudCheck records associated with expired payments", deletedFraudChecks);

            // 2. Delete dependents of soft-deleted client ledger entries
            int deletedLedgerEntries = entityManager.createQuery(
                    "DELETE FROM ClientLedgerEntry le WHERE le.isDeleted = true AND le.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} ClientLedgerEntry records", deletedLedgerEntries);

            // 3. Delete dependents of soft-deleted invoices (InvoiceItems)
            int deletedInvoiceItems = entityManager.createQuery(
                    "DELETE FROM InvoiceItem ii WHERE ii.invoice.isDeleted = true AND ii.invoice.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} InvoiceItem records", deletedInvoiceItems);

            // 4. Delete payments
            int deletedPayments = entityManager.createQuery(
                    "DELETE FROM Payment p WHERE p.isDeleted = true AND p.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} soft-deleted Payment records", deletedPayments);

            // 5. Delete invoices
            int deletedInvoices = entityManager.createQuery(
                    "DELETE FROM Invoice i WHERE i.isDeleted = true AND i.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} soft-deleted Invoice records", deletedInvoices);

            // 6. Delete client work items
            int deletedClientWorks = entityManager.createQuery(
                    "DELETE FROM ClientWork cw WHERE cw.isDeleted = true AND cw.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} soft-deleted ClientWork records", deletedClientWorks);

            // 7. Delete clients
            int deletedClients = entityManager.createQuery(
                    "DELETE FROM Client c WHERE c.isDeleted = true AND c.deletedAt < :cutoff"
            ).setParameter("cutoff", softDeleteCutoff).executeUpdate();
            log.info("Pruned {} soft-deleted Client records", deletedClients);

            // 8. Delete expired activity logs
            int deletedActivityLogs = entityManager.createQuery(
                    "DELETE FROM ActivityLog al WHERE al.isDeleted = true OR al.createdAt < :cutoff"
            ).setParameter("cutoff", logCutoff).executeUpdate();
            log.info("Pruned {} expired ActivityLog records", deletedActivityLogs);

            // 9. Delete expired entity change history
            int deletedChangeHistory = entityManager.createQuery(
                    "DELETE FROM EntityChangeHistory ech WHERE ech.timestamp < :cutoff"
            ).setParameter("cutoff", logCutoff).executeUpdate();
            log.info("Pruned {} expired EntityChangeHistory records", deletedChangeHistory);

            log.info("GDPR data retention cleanup process completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during GDPR data retention execution", e);
        }
    }
}