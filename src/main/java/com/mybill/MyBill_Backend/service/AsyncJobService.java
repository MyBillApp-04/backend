package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.entity.AsyncJob;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.AsyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    @Transactional
    public AsyncJob enqueue(String jobType, Object payload, User user, UUID invoiceId) {
        try {
            String payloadStr = objectMapper.writeValueAsString(payload);
            AsyncJob job = AsyncJob.builder()
                    .jobType(jobType)
                    .payload(payloadStr)
                    .status("PENDING")
                    .attemptCount(0)
                    .maxAttempts(5)
                    .nextRunAt(LocalDateTime.now())
                    .user(user)
                    .invoiceId(invoiceId)
                    .build();
            return asyncJobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to enqueue async job of type {}", jobType, e);
            throw new RuntimeException("Failed to enqueue async job", e);
        }
    }

    @Transactional
    public void executeJob(AsyncJob job) {
        job.setStatus("RUNNING");
        asyncJobRepository.saveAndFlush(job);

        try {
            log.info("Starting execution of async job: ID={}, Type={}", job.getJobId(), job.getJobType());

            switch (job.getJobType()) {
                case "EMAIL" -> executeEmailJob(job);
                case "GOOGLE_DRIVE_BACKUP" -> executeGoogleDriveBackupJob(job);
                case "STRIPE_PAYMENT" -> executeStripePaymentJob(job);
                default -> throw new IllegalArgumentException("Unknown job type: " + job.getJobType());
            }

            job.setStatus("COMPLETED");
            job.setLastError(null);
            log.info("Successfully completed async job: ID={}", job.getJobId());
        } catch (Exception e) {
            log.error("Failed executing async job: ID={}", job.getJobId(), e);
            int attempts = job.getAttemptCount() + 1;
            job.setAttemptCount(attempts);
            job.setLastError(e.getMessage());

            if (attempts >= job.getMaxAttempts()) {
                job.setStatus("DEAD");
                log.warn("Job ID={} moved to Dead Letter Queue (DLQ) after {} attempts", job.getJobId(), attempts);
            } else {
                job.setStatus("FAILED");
                long backoffSeconds = (long) Math.min(60.0, Math.pow(2, attempts));
                job.setNextRunAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.info("Rescheduling Job ID={} for retry in {}s at {}", job.getJobId(), backoffSeconds, job.getNextRunAt());
            }
        }
        asyncJobRepository.save(job);
    }

    private void executeEmailJob(AsyncJob job) throws Exception {
        EmailService emailService = applicationContext.getBean(EmailService.class);
        Map<?, ?> map = objectMapper.readValue(job.getPayload(), Map.class);

        String recipient = (String) map.get("recipient");
        String subject = (String) map.get("subject");
        String body = (String) map.get("body");
        String templateType = (String) map.get("templateType");
        Boolean attachInvoicePdf = (Boolean) map.get("attachInvoicePdf");

        com.mybill.MyBill_Backend.entity.EmailLog logEntity = com.mybill.MyBill_Backend.entity.EmailLog.builder()
                .user(job.getUser())
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(com.mybill.MyBill_Backend.entity.EmailStatus.PENDING)
                .templateType(templateType)
                .invoiceId(job.getInvoiceId())
                .attemptCount(0)
                .build();

        logEntity = applicationContext.getBean(com.mybill.MyBill_Backend.repository.EmailLogRepository.class).save(logEntity);
        emailService.attemptSend(logEntity, Boolean.TRUE.equals(attachInvoicePdf));

        if (logEntity.getStatus() == com.mybill.MyBill_Backend.entity.EmailStatus.FAILED) {
            throw new RuntimeException("Email delivery failed: " + logEntity.getErrorMessage());
        }
    }

    private void executeGoogleDriveBackupJob(AsyncJob job) throws Exception {
        Map<?, ?> map = objectMapper.readValue(job.getPayload(), Map.class);
        com.mybill.MyBill_Backend.service.backup.GoogleDriveBackupStorageClient googleDriveClient = 
                applicationContext.getBean(com.mybill.MyBill_Backend.service.backup.GoogleDriveBackupStorageClient.class);

        UUID backupJobId = UUID.fromString((String) map.get("backupJobId"));
        String localFilePath = (String) map.get("localFilePath");
        String expectedSha256 = (String) map.get("sha256");

        com.mybill.MyBill_Backend.repository.BackupJobRepository repo = 
                applicationContext.getBean(com.mybill.MyBill_Backend.repository.BackupJobRepository.class);

        com.mybill.MyBill_Backend.entity.BackupJob backupJob = repo.findById(backupJobId)
                .orElseThrow(() -> new IllegalArgumentException("BackupJob not found: " + backupJobId));

        java.nio.file.Path path = java.nio.file.Path.of(localFilePath);
        if (!java.nio.file.Files.exists(path)) {
            throw new java.io.FileNotFoundException("Local backup file not found: " + localFilePath);
        }

        if (expectedSha256 == null || expectedSha256.isBlank()) {
            expectedSha256 = com.mybill.MyBill_Backend.service.backup.BackupChecksum.sha256Hex(path);
        }

        String location = googleDriveClient.store(backupJob, path, expectedSha256);
        backupJob.setLocation(location);
        backupJob.setSha256(expectedSha256);
        backupJob.setStatus(com.mybill.MyBill_Backend.entity.BackupStatus.COMPLETED);
        backupJob.setCompletedAt(LocalDateTime.now());
        repo.save(backupJob);
    }

    private void executeStripePaymentJob(AsyncJob job) throws Exception {
        StripeService stripeService = applicationContext.getBean(StripeService.class);
        Map<?, ?> map = objectMapper.readValue(job.getPayload(), Map.class);

        String paymentIntentId = (String) map.get("paymentIntentId");
        Double amount = (Double) map.get("amount");

        stripeService.processPayment(paymentIntentId, amount);
    }
}
