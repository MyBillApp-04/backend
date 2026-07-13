package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.AsyncJob;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.repository.AsyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncJobScheduler {

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobService asyncJobService;
    private final DatabaseLockService databaseLockService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${app.async-jobs.poll-batch-size:5}")
    private int pollBatchSize;

    @Value("${app.async-jobs.running-timeout-minutes:15}")
    private long runningTimeoutMinutes;

    @Scheduled(fixedDelay = 10000)
    public void pollAndProcessJobs() {
        if (!running.compareAndSet(false, true)) {
            log.debug("Skipping async job poll because the previous poll is still running");
            return;
        }
        if (!databaseLockService.tryLock(DatabaseLockService.ASYNC_JOB_SCHEDULER)) {
            log.debug("Skipping async job poll because another instance owns the database scheduler lock");
            running.set(false);
            return;
        }

        try {
        log.debug("Polling for executable async retry jobs...");
        recoverInterruptedJobs();

        List<AsyncJob> jobs = asyncJobRepository.findExecutableJobs(
                List.of("PENDING", "FAILED"),
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(1, pollBatchSize))
        );

        if (!jobs.isEmpty()) {
            log.info("Found {} async retry jobs to execute", jobs.size());
            for (AsyncJob job : jobs) {
                try {
                    asyncJobService.executeJob(job);
                } catch (Exception e) {
                    log.error("Unhandled error processing job: ID={} exception={} message={}",
                            job.getJobId(), e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
                }
            }
        }
        } finally {
            databaseLockService.unlock(DatabaseLockService.ASYNC_JOB_SCHEDULER);
            running.set(false);
        }
    }

    private void recoverInterruptedJobs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(Duration.ofMinutes(Math.max(1, runningTimeoutMinutes)));
        int recovered = asyncJobRepository.recoverStaleRunningJobs(
                cutoff,
                now,
                "Recovered stale RUNNING job after worker interruption"
        );
        if (recovered > 0) {
            log.warn("Recovered {} stale RUNNING async job(s) for retry", recovered);
        }
    }
}
