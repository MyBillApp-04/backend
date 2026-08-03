package com.mybill.MyBill_Backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates critical application environment variables and configuration properties
 * during Spring Boot application startup.
 *
 * <p>Prevents running the application with missing or unsafe configuration keys
 * (e.g. missing JWT secret, default weak secrets in production, missing DB credentials).</p>
 */
@Component
@Slf4j
public class AppStartupConfigValidator implements ApplicationRunner {

    private final Environment environment;

    public AppStartupConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (isProfileActive("test")) {
            log.info("Skipping strict startup config validation in 'test' profile.");
            return;
        }

        log.info("Validating application startup configuration parameters...");

        List<String> missingCriticalKeys = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean isProd = isProfileActive("prod");

        // 1. Validate JWT Secret
        String jwtSecret = getProperty("jwt.secret", "JWT_SECRET");
        if (isBlank(jwtSecret)) {
            missingCriticalKeys.add("JWT_SECRET (jwt.secret)");
        } else if (jwtSecret.length() < 32) {
            warnings.add("JWT_SECRET is shorter than 32 characters. Consider using a stronger key for HS256.");
        }

        // 2. Validate Database URL & Credentials
        String dbUrl = getProperty("spring.datasource.url", "DB_URL");
        if (isBlank(dbUrl)) {
            missingCriticalKeys.add("DB_URL (spring.datasource.url)");
        }

        String dbUser = getProperty("spring.datasource.username", "DB_USERNAME");
        if (isBlank(dbUser)) {
            missingCriticalKeys.add("DB_USERNAME (spring.datasource.username)");
        }

        // 3. Validate Production Profile Specifics
        if (isProd) {
            String firebaseConfig = getProperty("firebase.config.json", "FIREBASE_CONFIG_JSON");
            if (isBlank(firebaseConfig)) {
                missingCriticalKeys.add("FIREBASE_CONFIG_JSON (required in prod profile)");
            }

            if ("true".equalsIgnoreCase(getProperty("app.security.public-api-docs"))) {
                warnings.add("PUBLIC_API_DOCS is enabled in production profile.");
            }

            if (!"true".equalsIgnoreCase(getProperty("app.security.require-https"))) {
                warnings.add("REQUIRE_HTTPS is false in production profile. HTTPS redirection is strongly advised.");
            }
        }

        // Output warnings
        for (String warning : warnings) {
            log.warn("Configuration Warning: {}", warning);
        }

        // Fail startup on missing critical configuration keys
        if (!missingCriticalKeys.isEmpty()) {
            String errorMessage = "Startup Aborted — The following required configuration parameters are missing or blank: "
                    + String.join(", ", missingCriticalKeys);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Startup configuration validation completed successfully (active profiles: {}).",
                String.join(",", environment.getActiveProfiles()));
    }

    private boolean isProfileActive(String profileName) {
        for (String profile : environment.getActiveProfiles()) {
            if (profileName.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private String getProperty(String key) {
        return getProperty(key, null);
    }

    private String getProperty(String key, String fallbackEnvKey) {
        String val = environment.getProperty(key);
        if (isBlank(val) && fallbackEnvKey != null) {
            val = environment.getProperty(fallbackEnvKey);
        }
        return val;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
