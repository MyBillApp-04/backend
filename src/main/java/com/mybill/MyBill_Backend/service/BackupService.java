package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.BackupRequest;
import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.BackupJobRepository;
import com.mybill.MyBill_Backend.service.backup.BackupChecksum;
import com.mybill.MyBill_Backend.service.backup.BackupStorageClient;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupJobRepository backupJobRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final List<BackupStorageClient> storageClients;
    private final AsyncJobService asyncJobService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${app.backup.provider:LOCAL}")
    private BackupProvider defaultProvider;

    @Value("${app.backup.local-dir:backups}")
    private String backupDir;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
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
        payload.put("backupSchemaVersion", 3);
        payload.put("backupFormat", "named-column-json");
        payload.put("createdAt", LocalDateTime.now());
        payload.put("userId", userId);
        payload.put("user", rows("SELECT * FROM users WHERE id = :userId", userId));
        payload.put("businessProfiles", rows("SELECT * FROM business_profile WHERE user_id = :userId", userId));
        payload.put("invoiceSettings", rows("SELECT * FROM invoice_settings WHERE user_id = :userId", userId));
        payload.put("clients", rows("SELECT * FROM clients WHERE user_id = :userId", userId));
        payload.put("clientWork", rows("SELECT * FROM client_work WHERE user_id = :userId", userId));
        payload.put("invoices", rows("SELECT * FROM invoice WHERE user_id = :userId", userId));
        payload.put("invoiceItems", rows("SELECT * FROM invoice_items WHERE user_id = :userId", userId));
        payload.put("payments", rows("SELECT * FROM payments WHERE user_id = :userId", userId));
        payload.put("clientLedgerEntries", rows("SELECT * FROM client_ledger_entries WHERE user_id = :userId", userId));
        payload.put("expenses", rows("SELECT * FROM expenses WHERE user_id = :userId", userId));
        payload.put("recurringInvoiceSchedules", rows("SELECT * FROM recurring_invoice_schedule WHERE user_id = :userId", userId));
        payload.put("notifications", rows("SELECT * FROM notifications WHERE user_id = :userId", userId));
        payload.put("customerNotificationSettings", rows("SELECT * FROM customer_notification_settings WHERE user_id = :userId", userId));
        payload.put("customerNotificationTemplates", rows("SELECT * FROM customer_notification_templates WHERE user_id = :userId", userId));
        payload.put("customerNotificationLogs", rows("SELECT * FROM customer_notification_logs WHERE user_id = :userId", userId));
        payload.put("uploadedFiles", uploadedFiles(userId));

        Path dir = Path.of(backupDir);
        Files.createDirectories(dir);
        Path file = dir.resolve("mybill-backup-" + job.getBackupId() + ".json");
        Path tempFile = Files.createTempFile(dir, "mybill-backup-" + job.getBackupId() + "-", ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), payload);
            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
        return file;
    }

    private BackupStorageClient storageClient(BackupProvider provider) {
        return storageClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported backup provider: " + provider));
    }

    private List<Map<String, Object>> rows(String sql, Long userId) {
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("userId", userId));
    }

    private List<Map<String, Object>> uploadedFiles(Long userId) {
        List<Map<String, Object>> profiles = rows("""
                SELECT logo_path, qr_image_path, signature_path
                FROM business_profile
                WHERE user_id = :userId
                """, userId);
        if (profiles.isEmpty()) {
            return List.of();
        }

        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        return profiles.stream()
                .flatMap(row -> row.values().stream())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(path -> path.startsWith("/uploads/"))
                .distinct()
                .map(path -> uploadedFileEntry(path, root))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Map<String, Object>> uploadedFileEntry(String serverPath, Path root) {
        String filename = serverPath.substring("/uploads/".length());
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("path", serverPath);
            entry.put("filename", filename);
            entry.put("sha256", BackupChecksum.sha256Hex(file));
            entry.put("sizeBytes", Files.size(file));
            entry.put("contentBase64", Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
            return Optional.of(entry);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to include uploaded file in backup", exception);
        }
    }
}
