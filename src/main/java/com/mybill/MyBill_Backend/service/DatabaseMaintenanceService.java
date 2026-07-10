package com.mybill.MyBill_Backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void runVacuumAnalyze() {
        log.info("Starting scheduled database maintenance: VACUUM ANALYZE");
        try {
            jdbcTemplate.execute("VACUUM ANALYZE public.invoice");
            jdbcTemplate.execute("VACUUM ANALYZE public.payments");
            jdbcTemplate.execute("VACUUM ANALYZE public.clients");
            jdbcTemplate.execute("VACUUM ANALYZE public.client_work");
            jdbcTemplate.execute("VACUUM ANALYZE public.client_ledger");
            log.info("Database maintenance completed successfully.");
        } catch (Exception e) {
            log.error("Failed to run scheduled VACUUM ANALYZE. This is expected if running on H2 or lacking database superuser permissions.", e.getMessage());
        }
    }
}
