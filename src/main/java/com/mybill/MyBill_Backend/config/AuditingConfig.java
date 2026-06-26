package com.mybill.MyBill_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            // AuditorAware runs while Hibernate is flushing entities. Querying a
            // repository here causes an auto-flush, which invokes this callback
            // again and eventually overflows the stack. The JWT filter stores
            // the verified user id in authentication details for this purpose.
            Object details = authentication.getDetails();
            return details instanceof Long userId ? Optional.of(userId) : Optional.empty();
        };
    }
}
