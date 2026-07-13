package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.AsyncJob;
import com.mybill.MyBill_Backend.repository.AsyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AsyncJobAdminController {

    private final AsyncJobRepository asyncJobRepository;

    @Value("${app.async-jobs.dlq-alert-threshold:10}")
    private int dlqAlertThreshold;

    @Value("${app.async-jobs.admin-page-size:100}")
    private int adminPageSize;

    @GetMapping("/admin/async-jobs")
    public String getDashboard(
            @RequestParam(value = "status", required = false) String status,
            Model model
    ) {
        log.info("Accessing admin async jobs dashboard with status filter: {}", status);

        int pageSize = Math.max(1, adminPageSize);
        PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AsyncJob> jobs;
        if (status != null && !status.isBlank()) {
            jobs = asyncJobRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest).getContent();
        } else {
            jobs = asyncJobRepository.findAll(pageRequest).getContent();
        }

        long totalCount = asyncJobRepository.count();
        long pendingCount = asyncJobRepository.countByStatus("PENDING");
        long runningCount = asyncJobRepository.countByStatus("RUNNING");
        long completedCount = asyncJobRepository.countByStatus("COMPLETED");
        long failedCount = asyncJobRepository.countByStatus("FAILED");
        long deadCount = asyncJobRepository.countByStatus("DEAD");

        model.addAttribute("jobs", jobs);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("runningCount", runningCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("deadCount", deadCount);
        model.addAttribute("alertThreshold", dlqAlertThreshold);
        model.addAttribute("dlqAlert", deadCount > dlqAlertThreshold);
        model.addAttribute("selectedStatus", status);

        return "admin-jobs";
    }

    @PostMapping("/api/admin/async-jobs/{jobId}/retry")
    @ResponseBody
    public ResponseEntity<?> retryJob(@PathVariable("jobId") UUID jobId) {
        log.info("Requested manual retry for AsyncJob ID: {}", jobId);

        AsyncJob job = asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if ("COMPLETED".equals(job.getStatus())) {
            return ResponseEntity.badRequest().body("Cannot retry a completed job");
        }

        job.setStatus("PENDING");
        job.setAttemptCount(0);
        job.setNextRunAt(LocalDateTime.now());
        job.setLastError(null);
        asyncJobRepository.save(job);

        log.info("AsyncJob ID: {} successfully reset to PENDING status.", jobId);
        return ResponseEntity.ok().body(Map.of("message", "Job reset to PENDING successfully"));
    }

    @GetMapping("/api/admin/async-jobs/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        long deadCount = asyncJobRepository.countByStatus("DEAD");
        stats.put("total", asyncJobRepository.count());
        stats.put("pending", asyncJobRepository.countByStatus("PENDING"));
        stats.put("running", asyncJobRepository.countByStatus("RUNNING"));
        stats.put("completed", asyncJobRepository.countByStatus("COMPLETED"));
        stats.put("failed", asyncJobRepository.countByStatus("FAILED"));
        stats.put("dead", deadCount);
        stats.put("dlqAlertThreshold", dlqAlertThreshold);
        stats.put("dlqAlert", deadCount > dlqAlertThreshold);
        return ResponseEntity.ok(stats);
    }
}
