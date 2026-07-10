package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.BackupRequest;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.BackupJobRepository;
import com.mybill.MyBill_Backend.service.backup.BackupChecksum;
import com.mybill.MyBill_Backend.service.backup.BackupStorageClient;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupJobRepository backupJobRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final List<BackupStorageClient> storageClients;
    private final AsyncJobService asyncJobService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.backup.provider:LOCAL}")
    private BackupProvider defaultProvider;

    @Value("${app.backup.local-dir:backups}")
    private String backupDir;

    @Transactional
    public BackupJob createBackup(BackupRequest request) {
        BackupProvider provider = request != null && request.getProvider() != null
                ? request.getProvider()
                : defaultProvider;

        BackupJob job = BackupJob.builder()
                .user(securityUtils.getCurrentUser())
                .provider(provider)
                .status(BackupStatus.REQUESTED)
                .build();

        job = backupJobRepository.save(job);

        try {
            Path location = writeLocalBackup(job);
            String sha256 = BackupChecksum.sha256Hex(location);
            job.setSha256(sha256);

            if (provider == BackupProvider.GOOGLE_DRIVE) {
                job.setStatus(BackupStatus.REQUESTED);
                Map<String, Object> payload = Map.of(
                    "backupJobId", job.getBackupId().toString(),
                    "localFilePath", location.toAbsolutePath().toString(),
                    "sha256", sha256
                );
                asyncJobService.enqueue("GOOGLE_DRIVE_BACKUP", payload, job.getUser(), null);
            } else {
                String remoteLocation = storageClient(provider).store(job, location, sha256);
                job.setLocation(remoteLocation);
                job.setStatus(BackupStatus.COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
            }
        } catch (Exception exception) {
            job.setStatus(BackupStatus.FAILED);
            job.setErrorMessage(exception.getMessage());
        }

        return backupJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<BackupJob> listBackups() {
        return backupJobRepository.findByUserIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId());
    }

    private Path writeLocalBackup(BackupJob job) throws IOException {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("createdAt", LocalDateTime.now());
        payload.put("userId", userId);
        payload.put("clients", rows("SELECT * FROM clients WHERE user_id = :userId", userId));
        payload.put("clientWork", rows("SELECT * FROM client_work WHERE user_id = :userId", userId));
        payload.put("invoices", rows("SELECT * FROM invoice WHERE user_id = :userId", userId));
        payload.put("invoiceItems", rows("SELECT * FROM invoice_items WHERE user_id = :userId", userId));
        payload.put("payments", rows("SELECT * FROM payments WHERE user_id = :userId", userId));

        Path dir = Path.of(backupDir);
        Files.createDirectories(dir);
        Path file = dir.resolve("mybill-backup-" + job.getBackupId() + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), payload);
        return file;
    }

    private BackupStorageClient storageClient(BackupProvider provider) {
        return storageClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported backup provider: " + provider));
    }

    private List<?> rows(String sql, Long userId) {
        return entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
    }
}
