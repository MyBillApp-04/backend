package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.AsyncJob;
import com.mybill.MyBill_Backend.repository.AsyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncJobScheduler {

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobService asyncJobService;

    @Scheduled(fixedDelay = 10000)
    public void pollAndProcessJobs() {
        log.debug("Polling for executable async retry jobs...");

        List<AsyncJob> jobs = asyncJobRepository.findTop10ByStatusInAndNextRunAtLessThanEqualOrderByCreatedAtAsc(
                List.of("PENDING", "FAILED"),
                LocalDateTime.now()
        );

        if (!jobs.isEmpty()) {
            log.info("Found {} async retry jobs to execute", jobs.size());
            for (AsyncJob job : jobs) {
                try {
                    asyncJobService.executeJob(job);
                } catch (Exception e) {
                    log.error("Unhandled error processing job ID: {}", job.getJobId(), e);
                }
            }
        }
    }
}
