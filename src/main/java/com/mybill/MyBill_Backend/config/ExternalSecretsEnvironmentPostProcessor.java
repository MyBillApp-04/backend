package com.mybill.MyBill_Backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the local shared secrets file available before Spring resolves datasource,
 * Flyway, and Firebase properties. This intentionally parses dotenv syntax rather
 * than Java properties syntax, preserving backslashes in service-account JSON.
 */
public class ExternalSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "externalSecrets";
    private static final String DEFAULT_SECRETS_FILE = "C:\\src\\Secrets\\.env";
    private static final String SECRETS_FILE_ENV_KEY = "MYBILL_SECRETS_FILE";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path secretsFile = Path.of(resolveSecretsFile());
        if (!Files.isRegularFile(secretsFile)) {
            return;
        }

        Map<String, Object> properties = readDotenvFile(secretsFile);
        if (!properties.isEmpty()) {
            // Real process environment variables remain the highest-priority source.
            environment.getPropertySources().addAfter(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(PROPERTY_SOURCE_NAME, properties)
            );
        }
    }

    private String resolveSecretsFile() {
        String configuredPath = System.getenv(SECRETS_FILE_ENV_KEY);
        return configuredPath == null || configuredPath.isBlank() ? DEFAULT_SECRETS_FILE : configuredPath;
    }

    private Map<String, Object> readDotenvFile(Path secretsFile) {
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(secretsFile);
            for (String line : lines) {
                parseLine(line, properties);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read secrets file: " + secretsFile, exception);
        }
        return properties;
    }

    private void parseLine(String line, Map<String, Object> properties) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
            return;
        }

        if (trimmedLine.startsWith("export ")) {
            trimmedLine = trimmedLine.substring("export ".length()).trim();
        }

        int separatorIndex = trimmedLine.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = trimmedLine.substring(0, separatorIndex).trim();
        String value = trimmedLine.substring(separatorIndex + 1).trim();
        if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            properties.put(key, stripWrappingQuotes(value));
        }
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
