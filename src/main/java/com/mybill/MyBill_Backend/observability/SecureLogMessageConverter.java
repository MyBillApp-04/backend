package com.mybill.MyBill_Backend.observability;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

public class SecureLogMessageConverter extends ClassicConverter {

    private static final String REDACTED = "$1=<redacted>";
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(authorization)\\s*[:=]\\s*Bearer\\s+[^\\s,;]+"),
            Pattern.compile("(?i)\\b(bearer)\\s+[A-Za-z0-9._~+/-]+=*"),
            Pattern.compile("(?i)\\b(password|passwd|pwd|secret|jwt|token|id_token|access_token|refresh_token|api_key|apikey|firebase_config_json|google_drive_access_token)\\b\\s*[:=]\\s*[^\\s,;]+"),
            Pattern.compile("(?i)\\\"(password|passwd|pwd|secret|jwt|token|id_token|access_token|refresh_token|apiKey|api_key|firebaseConfigJson)\\\"\\s*:\\s*\\\"[^\\\"]*\\\""),
            Pattern.compile("(?i)(Authorization: Bearer)\\s+[^\\s]+")
    );

    @Override
    public String convert(ILoggingEvent event) {
        return sanitize(event.getFormattedMessage());
    }

    public static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String sanitized = message;
        for (Pattern pattern : SECRET_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll(REDACTED);
        }
        return sanitized;
    }
}
