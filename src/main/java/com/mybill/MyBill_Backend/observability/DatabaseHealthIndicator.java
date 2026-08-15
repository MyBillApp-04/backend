package com.mybill.MyBill_Backend.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the primary database.
 *
 * <p>Exposed under {@code /actuator/health/database}. Reports {@code UP} only
 * when a trivial SQL query executes successfully within the connection pool.</p>
 */
@Component("database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("check", "SELECT 1 passed")
                        .build();
            }
            return Health.down()
                    .withDetail("check", "SELECT 1 returned unexpected result: " + result)
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
