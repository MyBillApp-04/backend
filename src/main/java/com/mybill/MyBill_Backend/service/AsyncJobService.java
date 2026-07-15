package com.mybill.MyBill_Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.entity.AsyncJob;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
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
            log.error("Failed to enqueue async job: type={} exception={} message={}",
                    jobType, e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
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
                case "STRIPE_PAYMENT" -> executeStripePaymentJob(job);
                default -> throw new IllegalArgumentException("Unknown job type: " + job.getJobType());
            }

            job.setStatus("COMPLETED");
            job.setLastError(null);
            log.info("Successfully completed async job: ID={}", job.getJobId());
        } catch (Exception e) {
            log.error("Failed executing async job: ID={} exception={} message={}",
                    job.getJobId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
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

    private void executeStripePaymentJob(AsyncJob job) throws Exception {
        StripeService stripeService = applicationContext.getBean(StripeService.class);
        Map<?, ?> map = objectMapper.readValue(job.getPayload(), Map.class);

        String paymentIntentId = (String) map.get("paymentIntentId");
        Double amount = (Double) map.get("amount");

        stripeService.processPayment(paymentIntentId, amount);
    }
}
