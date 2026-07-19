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
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (FlywayValidateException ex) {
                if (!isKnownChecksumMismatch(ex)) {
                    throw ex;
                }

                log.info("Repairing known Flyway checksum mismatch before migration.");
                log.debug("Flyway checksum mismatch details: {}", ex.getMessage());
                flyway.repair();
                flyway.migrate();
            }
        };
    }

    private boolean isKnownChecksumMismatch(FlywayValidateException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("Migration checksum mismatch") || message.contains("checksum mismatch"));
    }
}
