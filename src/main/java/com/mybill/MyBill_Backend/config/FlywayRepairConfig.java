package com.mybill.MyBill_Backend.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);
    private static final String V39_CHECKSUM_MISMATCH = "Migration checksum mismatch for migration version 39";

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (FlywayValidateException ex) {
                if (!isKnownV39ChecksumMismatch(ex)) {
                    throw ex;
                }

                log.warn("Repairing known Flyway V39 checksum mismatch before applying follow-up migrations.");
                flyway.repair();
                flyway.migrate();
            }
        };
    }

    private boolean isKnownV39ChecksumMismatch(FlywayValidateException ex) {
        String message = ex.getMessage();
        return message != null && message.contains(V39_CHECKSUM_MISMATCH);
    }
}
