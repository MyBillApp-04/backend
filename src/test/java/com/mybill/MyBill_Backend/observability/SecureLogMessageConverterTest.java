package com.mybill.MyBill_Backend.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureLogMessageConverterTest {

    @Test
    void sanitizeRedactsAuthorizationBearerTokens() {
        String sanitized = SecureLogMessageConverter.sanitize(
                "Authorization: Bearer eyJhbGciOiJIUzI1Ni.test.signature");

        assertThat(sanitized).contains("<redacted>");
        assertThat(sanitized).doesNotContain("eyJhbGciOiJIUzI1Ni");
    }

    @Test
    void sanitizeRedactsCommonSecretFields() {
        String sanitized = SecureLogMessageConverter.sanitize(
                "password=hunter2 access_token=abc123 firebase_config_json={secret}");

        assertThat(sanitized).contains("password=<redacted>");
        assertThat(sanitized).contains("access_token=<redacted>");
        assertThat(sanitized).contains("firebase_config_json=<redacted>");
        assertThat(sanitized).doesNotContain("hunter2");
        assertThat(sanitized).doesNotContain("abc123");
        assertThat(sanitized).doesNotContain("{secret}");
    }
}
